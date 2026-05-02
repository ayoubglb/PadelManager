package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.match.MesMatchsDTO;
import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — consulter mes matchs")
class MatchServiceMesMatchsTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;

    @Mock private be.ephec.padelmanager.repository.TransactionRepository transactionRepository;
    @Mock private be.ephec.padelmanager.repository.TerrainRepository terrainRepository;
    @Mock private be.ephec.padelmanager.repository.HoraireSiteRepository horaireSiteRepository;
    @Mock private be.ephec.padelmanager.repository.JourFermetureRepository jourFermetureRepository;
    @Mock private be.ephec.padelmanager.repository.UtilisateurRepository utilisateurRepository;
    @Mock private SoldeService soldeService;
    @Mock private be.ephec.padelmanager.mapper.MatchMapper matchMapper;
    @Mock private be.ephec.padelmanager.mapper.InscriptionMatchMapper inscriptionMatchMapper;
    @Mock private be.ephec.padelmanager.mapper.TransactionMapper transactionMapper;

    private final Clock clockFige = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("UTC"));

    private MatchService matchService;

    private Site site;
    private Terrain terrain;
    private Utilisateur joueur, organisateur;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository, inscriptionMatchRepository, transactionRepository,
                terrainRepository, horaireSiteRepository, jourFermetureRepository,
                soldeService, matchMapper, clockFige,
                utilisateurRepository, inscriptionMatchMapper, transactionMapper);

        site = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        terrain = Terrain.builder().id(10L).numero(1).nom("T1").site(site).active(true).build();
        organisateur = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();
        joueur = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();
    }

    private Match creerMatch(Long id, LocalDateTime debut, Utilisateur orga) {
        return Match.builder()
                .id(id).terrain(terrain)
                .dateHeureDebut(debut).dateHeureFin(debut.plusMinutes(90))
                .organisateur(orga)
                .type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .build();
    }

    private InscriptionMatch creerInscription(Long id, Match match, Utilisateur j, boolean paye, boolean estOrga) {
        return InscriptionMatch.builder()
                .id(id).match(match).joueur(j)
                .paye(paye).statut(StatutInscription.INSCRIT)
                .estOrganisateur(estOrga).build();
    }

    // ─── Cas nominaux ────────────────────────────────────────────

    @Test
    @DisplayName("consulterMesMatchs aVenir=true retourne les matchs futurs avec mon rôle et paiement")
    void mesMatchsAVenir() {
        Match m1 = creerMatch(50L, LocalDateTime.of(2026, 6, 5, 14, 0), joueur);     // organisateur
        Match m2 = creerMatch(51L, LocalDateTime.of(2026, 6, 10, 10, 0), organisateur); // invité
        InscriptionMatch insc1 = creerInscription(1L, m1, joueur, true, true);
        InscriptionMatch insc2 = creerInscription(2L, m2, joueur, false, false);

        when(matchRepository.findMesMatchs(eq200L(), anyBoolean(), any())).thenReturn(List.of(m1, m2));
        when(inscriptionMatchRepository.findByMatchIdInAndJoueurId(any(), eq200L()))
                .thenReturn(List.of(insc1, insc2));
        lenient().when(inscriptionMatchRepository.findInscritsByMatchId(anyLong()))
                .thenReturn(List.of(insc1));  // 1 inscrit pour simplifier

        List<MesMatchsDTO> resultats = matchService.consulterMesMatchs(joueur, true);

        assertThat(resultats).hasSize(2);
        // Tri ASC pour aVenir=true : m1 (5/6) avant m2 (10/6)
        assertThat(resultats.get(0).id()).isEqualTo(50L);
        assertThat(resultats.get(0).monRole()).isEqualTo(MesMatchsDTO.MonRole.ORGANISATEUR);
        assertThat(resultats.get(0).maPartPayee()).isTrue();
        assertThat(resultats.get(1).id()).isEqualTo(51L);
        assertThat(resultats.get(1).monRole()).isEqualTo(MesMatchsDTO.MonRole.INVITE);
        assertThat(resultats.get(1).maPartPayee()).isFalse();
    }

    @Test
    @DisplayName("consulterMesMatchs aVenir=false retourne les matchs passés triés DESC")
    void mesMatchsPasses() {
        Match m1 = creerMatch(50L, LocalDateTime.of(2026, 5, 20, 14, 0), joueur);
        Match m2 = creerMatch(51L, LocalDateTime.of(2026, 5, 15, 10, 0), joueur);
        InscriptionMatch insc1 = creerInscription(1L, m1, joueur, true, true);
        InscriptionMatch insc2 = creerInscription(2L, m2, joueur, true, true);

        when(matchRepository.findMesMatchs(eq200L(), anyBoolean(), any()))
                .thenReturn(List.of(m1, m2));
        when(inscriptionMatchRepository.findByMatchIdInAndJoueurId(any(), eq200L()))
                .thenReturn(List.of(insc1, insc2));
        lenient().when(inscriptionMatchRepository.findInscritsByMatchId(anyLong()))
                .thenReturn(List.of(insc1));

        List<MesMatchsDTO> resultats = matchService.consulterMesMatchs(joueur, false);

        assertThat(resultats).hasSize(2);
        // Tri DESC pour aVenir=false : m1 (20/5) avant m2 (15/5)
        assertThat(resultats.get(0).id()).isEqualTo(50L);
        assertThat(resultats.get(1).id()).isEqualTo(51L);
    }

    @Test
    @DisplayName("consulterMesMatchs aucun match → liste vide")
    void aucunMatch() {
        when(matchRepository.findMesMatchs(eq200L(), anyBoolean(), any()))
                .thenReturn(List.of());

        List<MesMatchsDTO> resultats = matchService.consulterMesMatchs(joueur, true);

        assertThat(resultats).isEmpty();
    }

    // Helper pour matcher l'id 200L (lisibilité)
    private Long eq200L() {
        return org.mockito.ArgumentMatchers.eq(200L);
    }
}