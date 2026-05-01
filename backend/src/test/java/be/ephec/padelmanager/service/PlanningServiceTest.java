package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.planning.*;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.PlanningMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de PlanningService
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanningService — assemblage de la grille planning")
class PlanningServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private GenerateurCreneau generateurCreneau;
    @Mock private PlanningMapper planningMapper;

    @InjectMocks
    private PlanningService planningService;

    private Site site;
    private Terrain t1, t2;
    private Utilisateur membreLibre, membreSiteAnderlecht, organisateur;
    private final LocalDate date = LocalDate.of(2026, 4, 30);

    @BeforeEach
    void setUp() {
        site = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        t1 = Terrain.builder().id(10L).numero(1).nom("T1").site(site).active(true).build();
        t2 = Terrain.builder().id(11L).numero(2).nom("T2").site(site).active(true).build();

        membreLibre = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        membreSiteAnderlecht = Utilisateur.builder()
                .id(101L).matricule("S200001").nom("Smith").prenom("Anna")
                .role(RoleUtilisateur.MEMBRE_SITE).active(true).siteRattachement(site).build();

        organisateur = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        // Stubs lenient sur le mapper : pas tous les tests les utilisent
        lenient().when(planningMapper.toTerrainDto(any(Terrain.class)))
                .thenAnswer(invocation -> {
                    Terrain t = invocation.getArgument(0);
                    return new TerrainPlanningDTO(t.getId(), t.getNumero(), t.getNom());
                });
        lenient().when(planningMapper.nomOrganisateur(any(Match.class)))
                .thenReturn("Maria Lopez");
    }

    private void planningOuvertAvecDeuxCreneaux() {
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(jourFermetureRepository.findGlobaleParDate(date)).thenReturn(Optional.empty());
        when(jourFermetureRepository.findParSiteEtDate(1L, date)).thenReturn(Optional.empty());
        when(terrainRepository.findBySiteIdAndActiveTrueOrderByNumeroAsc(1L))
                .thenReturn(List.of(t1, t2));
        when(generateurCreneau.genererCreneaux(1L, 2026)).thenReturn(List.of(
                new CreneauDTO(LocalTime.of(8, 0),  LocalTime.of(9, 30)),
                new CreneauDTO(LocalTime.of(9, 45), LocalTime.of(11, 15))
        ));
    }

    // ─── Cas nominal et statuts ─────────────────────────────────────────

    @Test
    @DisplayName("Aucun match ce jour → toutes les cellules sont LIBRE")
    void aucunMatchToutesLibres() {
        planningOuvertAvecDeuxCreneaux();
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of());

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        assertThat(planning.ferme()).isFalse();
        assertThat(planning.terrains()).hasSize(2);
        assertThat(planning.creneaux()).hasSize(2);
        for (LigneCreneauDTO ligne : planning.creneaux()) {
            for (CelluleDTO cell : ligne.cellules()) {
                assertThat(cell.statut()).isEqualTo(StatutCellule.LIBRE);
                assertThat(cell.matchId()).isNull();
            }
        }
    }

    @Test
    @DisplayName("Match PRIVE → cellule PRIVE sans organisateur ni places")
    void matchPriveMasqueLesDetails() {
        planningOuvertAvecDeuxCreneaux();
        Match prive = Match.builder()
                .id(500L).terrain(t1)
                .dateHeureDebut(date.atTime(8, 0))
                .organisateur(organisateur)
                .type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .build();
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of(prive));
        when(inscriptionMatchRepository.countJoueursPayesByMatchIdIn(List.of(500L)))
                .thenReturn(List.of());

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        CelluleDTO cellule = planning.creneaux().get(0).cellules().get(0);
        assertThat(cellule.statut()).isEqualTo(StatutCellule.PRIVE);
        assertThat(cellule.matchId()).isEqualTo(500L);
        assertThat(cellule.placesRestantes()).isNull();
        assertThat(cellule.organisateurNom()).isNull();
    }

    @Test
    @DisplayName("Match PUBLIC avec 2 payés → PUBLIC_DISPO + 2 places + organisateur")
    void matchPublicAvecPlacesAfficheDetails() {
        planningOuvertAvecDeuxCreneaux();
        Match pub = Match.builder()
                .id(600L).terrain(t1)
                .dateHeureDebut(date.atTime(8, 0))
                .organisateur(organisateur)
                .type(TypeMatch.PUBLIC).statut(StatutMatch.PROGRAMME)
                .build();
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of(pub));
        when(inscriptionMatchRepository.countJoueursPayesByMatchIdIn(List.of(600L)))
                .thenReturn(List.<Object[]>of(new Object[]{600L, 2L}));

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        CelluleDTO cellule = planning.creneaux().get(0).cellules().get(0);
        assertThat(cellule.statut()).isEqualTo(StatutCellule.PUBLIC_DISPO);
        assertThat(cellule.matchId()).isEqualTo(600L);
        assertThat(cellule.placesRestantes()).isEqualTo(2);
        assertThat(cellule.organisateurNom()).isEqualTo("Maria Lopez");
    }

    @Test
    @DisplayName("Match PUBLIC avec 4 payés → COMPLET (placesRestantes = 0)")
    void matchPublicCompletEstGrise() {
        planningOuvertAvecDeuxCreneaux();
        Match pub = Match.builder()
                .id(700L).terrain(t1)
                .dateHeureDebut(date.atTime(8, 0))
                .organisateur(organisateur)
                .type(TypeMatch.PUBLIC).statut(StatutMatch.PROGRAMME)
                .build();
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of(pub));
        when(inscriptionMatchRepository.countJoueursPayesByMatchIdIn(List.of(700L)))
                .thenReturn(List.<Object[]>of(new Object[]{700L, 4L}));

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        CelluleDTO cellule = planning.creneaux().get(0).cellules().get(0);
        assertThat(cellule.statut()).isEqualTo(StatutCellule.COMPLET);
        assertThat(cellule.placesRestantes()).isZero();
    }

    // ─── Fermetures ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Fermeture globale → planning marqué fermé, raison renvoyée, créneaux vide")
    void fermetureGlobaleRetourneFerme() {
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        JourFermeture jf = JourFermeture.builder()
                .id(1L).dateFermeture(date).site(null).raison("Maintenance générale").build();
        when(jourFermetureRepository.findGlobaleParDate(date)).thenReturn(Optional.of(jf));

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        assertThat(planning.ferme()).isTrue();
        assertThat(planning.raison()).isEqualTo("Maintenance générale");
        assertThat(planning.creneaux()).isEmpty();
        assertThat(planning.terrains()).isEmpty();
    }

    @Test
    @DisplayName("Fermeture site-spécifique → planning marqué fermé avec sa raison")
    void fermetureSiteRetourneFerme() {
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(jourFermetureRepository.findGlobaleParDate(date)).thenReturn(Optional.empty());
        JourFermeture jf = JourFermeture.builder()
                .id(2L).dateFermeture(date).site(site).raison("Tournoi privé").build();
        when(jourFermetureRepository.findParSiteEtDate(1L, date)).thenReturn(Optional.of(jf));

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        assertThat(planning.ferme()).isTrue();
        assertThat(planning.raison()).isEqualTo("Tournoi privé");
    }

    // ─── Autorisations ──────────────────────────────────────────────────

    @Test
    @DisplayName("Membre Site sur son site de rattachement → accès accepté")
    void membreSiteSurSonSiteAccepte() {
        planningOuvertAvecDeuxCreneaux();
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of());

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreSiteAnderlecht);

        assertThat(planning).isNotNull();
        assertThat(planning.siteId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Membre Site sur autre site → AccessDeniedException")
    void membreSiteSurAutreSiteRefuse() {
        Site autreSite = Site.builder().id(2L).nom("Forest").active(true).build();
        when(siteRepository.findById(2L)).thenReturn(Optional.of(autreSite));

        assertThatThrownBy(() ->
                planningService.consulterPlanning(2L, date, membreSiteAnderlecht))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("rattachement");
    }

    @Test
    @DisplayName("Site introuvable → EntityNotFoundException")
    void siteIntrouvable() {
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                planningService.consulterPlanning(999L, date, membreLibre))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Aucun horaire pour l'année → planning ouvert mais sans aucun créneau")
    void aucunHoraireProduitGrilleVide() {
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(jourFermetureRepository.findGlobaleParDate(date)).thenReturn(Optional.empty());
        when(jourFermetureRepository.findParSiteEtDate(1L, date)).thenReturn(Optional.empty());
        when(terrainRepository.findBySiteIdAndActiveTrueOrderByNumeroAsc(1L)).thenReturn(List.of(t1));
        when(generateurCreneau.genererCreneaux(1L, 2026)).thenReturn(List.of());
        when(matchRepository.findBySiteAndPeriode(eq(1L), any(), any())).thenReturn(List.of());

        PlanningDTO planning = planningService.consulterPlanning(1L, date, membreLibre);

        assertThat(planning.ferme()).isFalse();
        assertThat(planning.terrains()).hasSize(1);
        assertThat(planning.creneaux()).isEmpty();
    }
}