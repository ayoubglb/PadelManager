package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Tests d'intégration des requêtes custom de InscriptionMatchRepository sur SQL Server 2022
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("InscriptionMatchRepository — requêtes custom")
class InscriptionMatchRepositoryTest {

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mssql::getJdbcUrl);
        registry.add("spring.datasource.username", mssql::getUsername);
        registry.add("spring.datasource.password", mssql::getPassword);
        // Override Liquibase credentials pour qu'il utilise sa/password du container
        registry.add("spring.liquibase.url",      mssql::getJdbcUrl);
        registry.add("spring.liquibase.user",     mssql::getUsername);
        registry.add("spring.liquibase.password", mssql::getPassword);
    }

    @Autowired private TestEntityManager em;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;

    private Match match;
    private Utilisateur joueur1, joueur2;

    @BeforeEach
    void setUp() {
        joueur1 = creerJoueur("L999300", "joueur1.repo@padelmanager.be");
        joueur2 = creerJoueur("L999301", "joueur2.repo@padelmanager.be");

        Terrain terrain = em.getEntityManager()
                .createQuery("SELECT t FROM Terrain t WHERE t.site.id = 1 AND t.numero = 1", Terrain.class)
                .getSingleResult();

        match = em.persistAndFlush(Match.builder()
                .terrain(terrain)
                .dateHeureDebut(LocalDateTime.of(2026, 7, 20, 10, 0))
                .dateHeureFin(LocalDateTime.of(2026, 7, 20, 11, 30))
                .organisateur(joueur1)
                .type(TypeMatch.PRIVE)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());
    }

    private Utilisateur creerJoueur(String matricule, String email) {
        return em.persistAndFlush(Utilisateur.builder()
                .matricule(matricule)
                .email(email)
                .telephone("0000000000")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Test").prenom(matricule)
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());
    }

    private InscriptionMatch creer(Match m, Utilisateur joueur, boolean paye, StatutInscription statut) {
        return em.persistAndFlush(InscriptionMatch.builder()
                .match(m).joueur(joueur).paye(paye).statut(statut)
                .estOrganisateur(false).build());
    }

    // ─── countJoueursPayesByMatchId ──────────────────────────────────────

    @Test
    @DisplayName("countJoueursPayesByMatchId compte uniquement les inscriptions payées et INSCRIT")
    void countJoueursPayes() {
        creer(match, joueur1, true, StatutInscription.INSCRIT);   // ok
        creer(match, joueur2, false, StatutInscription.INSCRIT);  //  pas payé

        long count = inscriptionMatchRepository.countJoueursPayesByMatchId(match.getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("countJoueursPayesByMatchId retourne 0 sans inscription")
    void countJoueursPayesAucune() {
        long count = inscriptionMatchRepository.countJoueursPayesByMatchId(match.getId());

        assertThat(count).isZero();
    }

    // ─── findInscritsByMatchId ───────────────────────────────────────────

    @Test
    @DisplayName("findInscritsByMatchId retourne uniquement les statut INSCRIT")
    void findInscritsByMatchIdFiltreStatut() {
        InscriptionMatch insc = creer(match, joueur1, true, StatutInscription.INSCRIT);
        creer(match, joueur2, false, StatutInscription.ANNULE);  // exclu

        List<InscriptionMatch> resultats = inscriptionMatchRepository.findInscritsByMatchId(match.getId());

        assertThat(resultats).hasSize(1);
        assertThat(resultats.get(0).getId()).isEqualTo(insc.getId());
    }

    // ─── findUnpaidUpcomingWithin (job EF-SYS-002) ──────────────────────

    @Test
    @DisplayName("findUnpaidUpcomingWithin ne retourne que les non-payés futurs dans la fenêtre")
    void findUnpaidUpcomingWithin() {
        InscriptionMatch impayee = creer(match, joueur2, false, StatutInscription.INSCRIT);
        creer(match, joueur1, true, StatutInscription.INSCRIT);  // exclu : payé

        LocalDateTime debut = match.getDateHeureDebut().minusDays(1);
        LocalDateTime fin   = match.getDateHeureDebut().plusDays(1);

        List<InscriptionMatch> resultats = inscriptionMatchRepository.findUnpaidUpcomingWithin(debut, fin);

        assertThat(resultats).extracting(InscriptionMatch::getId).contains(impayee.getId());
        assertThat(resultats).allMatch(i -> !i.getPaye());
    }

    // ─── countJoueursPayesByMatchIdIn (utilisée par PlanningService) ────

    @Test
    @DisplayName("countJoueursPayesByMatchIdIn agrège par match avec IN + GROUP BY")
    void countJoueursPayesByMatchIdInAgregeParMatch() {
        creer(match, joueur1, true, StatutInscription.INSCRIT);
        creer(match, joueur2, true, StatutInscription.INSCRIT);

        List<Object[]> resultats = inscriptionMatchRepository
                .countJoueursPayesByMatchIdIn(List.of(match.getId()));

        assertThat(resultats).hasSize(1);
        assertThat((Long) resultats.get(0)[0]).isEqualTo(match.getId());
        assertThat((Long) resultats.get(0)[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("countJoueursPayesByMatchIdIn retourne liste vide si aucun match passé en argument")
    void countJoueursPayesByMatchIdInVide() {
        List<Object[]> resultats = inscriptionMatchRepository
                .countJoueursPayesByMatchIdIn(List.of(999_999L));

        assertThat(resultats).isEmpty();
    }
}