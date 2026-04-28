package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.dto.match.MatchDTO;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.MatchMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — création atomique avec validations")
class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private HoraireSiteRepository horaireSiteRepository;
    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private SoldeService soldeService;
    @Mock private MatchMapper matchMapper;

    private MatchService matchService;

    // Horloge figée au 15 mars 2026 à 10h00
    private final Clock clockFige = Clock.fixed(
            LocalDateTime.of(2026, 3, 15, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private Site siteAnderlecht;
    private Terrain terrain1;
    private Utilisateur membreLibre;
    private HoraireSite horaire2026;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository, inscriptionMatchRepository, transactionRepository,
                terrainRepository, horaireSiteRepository, jourFermetureRepository,
                soldeService, matchMapper, clockFige
        );

        siteAnderlecht = Site.builder()
                .id(1L).nom("Anderlecht").active(true).build();

        terrain1 = Terrain.builder()
                .id(10L).numero(1).site(siteAnderlecht).active(true).build();

        membreLibre = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        horaire2026 = HoraireSite.builder()
                .id(1L).site(siteAnderlecht).annee(2026)
                .heureDebut(LocalTime.of(8, 0))
                .heureFin(LocalTime.of(22, 0))
                .build();
    }

    // ─── Cas nominal ────────────────────────────────────────────────────

    @Test
    @DisplayName("creerMatch() crée Match + InscriptionMatch organisateur + Transaction PAIEMENT_MATCH")
    void creerMatchNominal() {
        // Match dans 3 jours à 14h (dans les 5 jours autorisés pour MEMBRE_LIBRE)
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        // Stubs
        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.of(horaire2026));
        when(matchRepository.existsByTerrainIdAndDateHeureDebut(10L, dateMatch))
                .thenReturn(false);
        when(soldeService.disposeAuMoinsDe(100L, PricingConstants.PART_JOUEUR))
                .thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setId(500L);
            return m;
        });
        when(matchMapper.toDto(any(Match.class))).thenReturn(
                new MatchDTO(500L, 10L, 1, 1L, "Anderlecht",
                        dateMatch, dateMatch.plusMinutes(90),
                        100L, "John Doe", TypeMatch.PRIVE, StatutMatch.PROGRAMME,
                        false, LocalDateTime.now(), false));

        MatchDTO result = matchService.creerMatch(requete, membreLibre);

        // Vérifie la création des 3 entités
        verify(matchRepository).save(any(Match.class));
        verify(inscriptionMatchRepository).save(argThat(insc ->
                insc.getJoueur().getId().equals(100L)
                        && Boolean.TRUE.equals(insc.getPaye())
                        && Boolean.TRUE.equals(insc.getEstOrganisateur())
                        && insc.getStatut() == StatutInscription.INSCRIT
        ));
        verify(transactionRepository).save(argThat(tx ->
                tx.getType() == TypeTransaction.PAIEMENT_MATCH
                        && tx.getMontant().compareTo(PricingConstants.PART_JOUEUR) == 0
                        && tx.getUtilisateur().getId().equals(100L)
        ));

        assertThat(result.id()).isEqualTo(500L);
    }

    // ─── Validations rejetées ───────────────────────────────────────────

    @Test
    @DisplayName("creerMatch() refuse si compte désactivé")
    void creerMatchRefuseSiCompteDesactive() {
        membreLibre.setActive(false);
        CreateMatchRequest requete = new CreateMatchRequest(
                10L, LocalDateTime.of(2026, 3, 18, 14, 0), TypeMatch.PRIVE);

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("désactivé");

        verifyNoInteractions(matchRepository, inscriptionMatchRepository, transactionRepository);
    }

    @Test
    @DisplayName("creerMatch() refuse si date pas dans le futur")
    void creerMatchRefuseSiDatePassee() {
        // Match daté du 14 mars (1 jour avant la date courante figée du 15 mars)
        CreateMatchRequest requete = new CreateMatchRequest(
                10L, LocalDateTime.of(2026, 3, 14, 14, 0), TypeMatch.PRIVE);

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    @DisplayName("creerMatch() refuse si MEMBRE_LIBRE réserve > 5 jours à l'avance")
    void creerMatchRefuseSiMembreLibreDelaiDepasse() {
        // Match dans 6 jours (CF-RV-002 : limite à 5 jours pour MEMBRE_LIBRE)
        CreateMatchRequest requete = new CreateMatchRequest(
                10L, LocalDateTime.of(2026, 3, 21, 14, 0), TypeMatch.PRIVE);

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 jours");
    }

    @Test
    @DisplayName("creerMatch() autorise MEMBRE_GLOBAL à 21 jours")
    void creerMatchAutoriseMembreGlobalA21Jours() {
        Utilisateur membreGlobal = Utilisateur.builder()
                .id(101L).matricule("G600001").nom("Smith").prenom("Jane")
                .role(RoleUtilisateur.MEMBRE_GLOBAL).active(true).build();

        // Match dans exactement 20 jours (dans la limite des 21j)
        LocalDateTime dateMatch = LocalDateTime.of(2026, 4, 4, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PUBLIC);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.of(horaire2026));
        when(matchRepository.existsByTerrainIdAndDateHeureDebut(10L, dateMatch))
                .thenReturn(false);
        when(soldeService.disposeAuMoinsDe(101L, PricingConstants.PART_JOUEUR))
                .thenReturn(true);
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchMapper.toDto(any(Match.class))).thenReturn(mock(MatchDTO.class));

        // Doit réussir (dans la limite des 21 jours)
        matchService.creerMatch(requete, membreGlobal);

        verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("creerMatch() refuse si MEMBRE_SITE réserve sur autre site")
    void creerMatchRefuseSiMembreSiteAutreSite() {
        Site siteForest = Site.builder().id(2L).nom("Forest").active(true).build();
        Utilisateur membreSite = Utilisateur.builder()
                .id(102L).matricule("S600001").nom("Smith").prenom("Bob")
                .role(RoleUtilisateur.MEMBRE_SITE).active(true)
                .siteRattachement(siteForest)
                .build();

        // Réservation sur Anderlecht (terrain1.site) alors que rattaché à Forest
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreSite))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("site de rattachement");
    }

    @Test
    @DisplayName("creerMatch() refuse si terrain inconnu (404)")
    void creerMatchRefuseSiTerrainInconnu() {
        CreateMatchRequest requete = new CreateMatchRequest(
                999L, LocalDateTime.of(2026, 3, 18, 14, 0), TypeMatch.PRIVE);

        when(terrainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("creerMatch() refuse si jour fermé")
    void creerMatchRefuseSiJourFerme() {
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fermé");
    }

    @Test
    @DisplayName("creerMatch() refuse si aucun horaire défini pour l'année")
    void creerMatchRefuseSiAucunHoraireAnnee() {
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun horaire");
    }

    @Test
    @DisplayName("creerMatch() refuse si créneau hors horaires d'ouverture")
    void creerMatchRefuseSiCreneauHorsHoraires() {
        // Match à 21h30 alors que le site ferme à 22h00
        // → 21h30 + 1h30 = 23h00 > 22h00 (fin après fermeture)
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 21, 30);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.of(horaire2026));

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horaires d'ouverture");
    }

    @Test
    @DisplayName("creerMatch() refuse si créneau déjà pris")
    void creerMatchRefuseSiCreneauDejaPris() {
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.of(horaire2026));
        when(matchRepository.existsByTerrainIdAndDateHeureDebut(10L, dateMatch))
                .thenReturn(true); // créneau déjà pris

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà réservé");
    }

    @Test
    @DisplayName("creerMatch() refuse si solde insuffisant")
    void creerMatchRefuseSiSoldeInsuffisant() {
        LocalDateTime dateMatch = LocalDateTime.of(2026, 3, 18, 14, 0);
        CreateMatchRequest requete = new CreateMatchRequest(10L, dateMatch, TypeMatch.PRIVE);

        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain1));
        when(jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(
                dateMatch.toLocalDate(), 1L)).thenReturn(false);
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026))
                .thenReturn(Optional.of(horaire2026));
        when(matchRepository.existsByTerrainIdAndDateHeureDebut(10L, dateMatch))
                .thenReturn(false);
        when(soldeService.disposeAuMoinsDe(100L, PricingConstants.PART_JOUEUR))
                .thenReturn(false);

        assertThatThrownBy(() -> matchService.creerMatch(requete, membreLibre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solde insuffisant");

        // Vérifie qu'aucune création ne s'est faite
        verify(matchRepository, never()).save(any());
        verify(inscriptionMatchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}