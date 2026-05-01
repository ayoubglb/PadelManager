package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.RoleUtilisateur;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Tests d'intégration des requêtes custom de MatchRepository sur SQL Server 2022
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("MatchRepository — requêtes custom")
class MatchRepositoryTest {

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
    @Autowired private MatchRepository matchRepository;

    private Utilisateur organisateur;
    private Terrain terrain;

    @BeforeEach
    void setUp() {
        organisateur = em.persistAndFlush(Utilisateur.builder()
                .matricule("L999200")
                .email("orga.match@padelmanager.be")
                .telephone("0000000000")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Orga").prenom("Match")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());

        terrain = em.getEntityManager()
                .createQuery("SELECT t FROM Terrain t WHERE t.site.id = 1 AND t.numero = 1", Terrain.class)
                .getSingleResult();
    }

    private Match creer(LocalDateTime debut, TypeMatch type, StatutMatch statut, boolean devenuPublic) {
        return em.persistAndFlush(Match.builder()
                .terrain(terrain)
                .dateHeureDebut(debut)
                .dateHeureFin(debut.plusMinutes(90))
                .organisateur(organisateur)
                .type(type)
                .statut(statut)
                .devenuPublicAutomatiquement(devenuPublic)
                .build());
    }

    // ─── findByIdForUpdate (verrou pessimiste) ───────────────────────────

    @Test
    @DisplayName("findByIdForUpdate retrouve un match existant")
    void findByIdForUpdateMatchExistant() {
        Match m = creer(LocalDateTime.of(2026, 6, 15, 10, 0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);

        Optional<Match> trouve = matchRepository.findByIdForUpdate(m.getId());

        assertThat(trouve).isPresent();
        assertThat(trouve.get().getId()).isEqualTo(m.getId());
    }

    @Test
    @DisplayName("findByIdForUpdate retourne empty pour un id inexistant")
    void findByIdForUpdateMatchInexistant() {
        Optional<Match> trouve = matchRepository.findByIdForUpdate(999_999L);

        assertThat(trouve).isEmpty();
    }

    // ─── findPublicsAVenir  ─────────────────────────

    @Test
    @DisplayName("findPublicsAVenir ne retourne que les matchs PUBLIC PROGRAMME futurs")
    void findPublicsAVenirFiltre() {
        LocalDateTime futur = LocalDateTime.now().plusDays(5);
        LocalDateTime passe = LocalDateTime.now().minusDays(1);

        Match publicFutur = creer(futur, TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        creer(futur.plusHours(2), TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);   // exclu : type
        creer(passe, TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);              // exclu : passé
        creer(futur.plusHours(4), TypeMatch.PUBLIC, StatutMatch.ANNULE, false);    // exclu : statut

        List<Match> resultats = matchRepository.findPublicsAVenir(LocalDateTime.now());

        assertThat(resultats).extracting(Match::getId).contains(publicFutur.getId());
        assertThat(resultats).allMatch(m -> m.getType() == TypeMatch.PUBLIC);
        assertThat(resultats).allMatch(m -> m.getStatut() == StatutMatch.PROGRAMME);
        assertThat(resultats).allMatch(m -> m.getDateHeureDebut().isAfter(LocalDateTime.now()));
    }

    // ─── findPrivesAConvertir  ───────────────────────────

    @Test
    @DisplayName("findPrivesAConvertir ne retourne que les PRIVE non-déjà-convertis dans la fenêtre")
    void findPrivesAConvertirFiltre() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limite = now.plusDays(7);

        Match prive = creer(now.plusDays(3).withHour(10).withMinute(0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);
        creer(now.plusDays(3).withHour(12).withMinute(0),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);          // exclu : type
        creer(now.plusDays(3).withHour(14).withMinute(0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, true);            // exclu : déjà converti
        creer(now.plusDays(10).withHour(10).withMinute(0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);           // exclu : hors fenêtre

        List<Match> resultats = matchRepository.findPrivesAConvertir(now, limite);

        assertThat(resultats).extracting(Match::getId).contains(prive.getId());
        assertThat(resultats).allMatch(m -> m.getType() == TypeMatch.PRIVE);
        assertThat(resultats).allMatch(m -> !m.getDevenuPublicAutomatiquement());
    }

    // ─── findBySiteAndPeriode (utilisée par PlanningService) ─────────────

    @Test
    @DisplayName("findBySiteAndPeriode filtre par site et période")
    void findBySiteAndPeriodeFiltre() {
        LocalDateTime debutFenetre = LocalDateTime.of(2026, 6, 15, 0, 0);
        LocalDateTime finFenetre   = LocalDateTime.of(2026, 6, 15, 23, 59, 59);

        Match dansFenetre = creer(LocalDateTime.of(2026, 6, 15, 10, 0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);
        creer(LocalDateTime.of(2026, 6, 16, 10, 0),
                TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);                // exclu : autre jour

        List<Match> resultats = matchRepository.findBySiteAndPeriode(1L, debutFenetre, finFenetre);

        assertThat(resultats).extracting(Match::getId).contains(dansFenetre.getId());
        assertThat(resultats).allMatch(m ->
                !m.getDateHeureDebut().isBefore(debutFenetre)
                        && !m.getDateHeureDebut().isAfter(finFenetre));
    }

    @Test
    @DisplayName("findBySiteAndPeriode exclut les matchs ANNULES")
    void findBySiteAndPeriodeExclutAnnules() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 15, 10, 0);

        Match annule = creer(debut, TypeMatch.PRIVE, StatutMatch.ANNULE, false);

        List<Match> resultats = matchRepository.findBySiteAndPeriode(1L,
                debut.minusHours(1), debut.plusHours(1));

        assertThat(resultats).extracting(Match::getId).doesNotContain(annule.getId());
    }
    // ─── rechercherPublics (match) ────────────────────────

    @Test
    @DisplayName("rechercherPublics ne retourne que les PUBLIC PROGRAMME futurs")
    void rechercherPublicsFiltreTypeEtStatut() {
        LocalDateTime futur = LocalDateTime.now().plusDays(5);

        Match publicProgramme = creer(futur,
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        creer(futur.plusHours(2), TypeMatch.PRIVE, StatutMatch.PROGRAMME, false);   // exclu : PRIVE
        creer(futur.plusHours(4), TypeMatch.PUBLIC, StatutMatch.ANNULE, false);    // exclu : ANNULE

        List<Match> resultats = matchRepository.rechercherPublics(
                LocalDateTime.now(), null, null);

        assertThat(resultats).extracting(Match::getId).contains(publicProgramme.getId());
        assertThat(resultats).allMatch(m -> m.getType() == TypeMatch.PUBLIC);
        assertThat(resultats).allMatch(m -> m.getStatut() == StatutMatch.PROGRAMME);
    }

    @Test
    @DisplayName("rechercherPublics filtre les matchs avant dateDebut")
    void rechercherPublicsFiltreDateDebut() {
        LocalDateTime dansUneSemaine = LocalDateTime.now().plusDays(7);
        LocalDateTime dansDeuxSemaines = LocalDateTime.now().plusDays(14);

        Match horsFenetre = creer(dansUneSemaine,
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        Match dansFenetre = creer(dansDeuxSemaines,
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);

        // dateDebut = dans 10 jours → exclut horsFenetre
        List<Match> resultats = matchRepository.rechercherPublics(
                LocalDateTime.now().plusDays(10), null, null);

        assertThat(resultats).extracting(Match::getId)
                .contains(dansFenetre.getId())
                .doesNotContain(horsFenetre.getId());
    }

    @Test
    @DisplayName("rechercherPublics filtre les matchs après dateFin")
    void rechercherPublicsFiltreDateFin() {
        LocalDateTime dansUneSemaine = LocalDateTime.now().plusDays(7);
        LocalDateTime dansTroisSemaines = LocalDateTime.now().plusDays(21);

        Match dansFenetre = creer(dansUneSemaine,
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        Match horsFenetre = creer(dansTroisSemaines,
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);

        // dateFin = dans 14 jours → exclut horsFenetre
        List<Match> resultats = matchRepository.rechercherPublics(
                LocalDateTime.now(), LocalDateTime.now().plusDays(14), null);

        assertThat(resultats).extracting(Match::getId)
                .contains(dansFenetre.getId())
                .doesNotContain(horsFenetre.getId());
    }

    @Test
    @DisplayName("rechercherPublics avec siteId null retourne tous les sites")
    void rechercherPublicsSansFiltreSite() {
        Match m = creer(LocalDateTime.now().plusDays(5),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);

        List<Match> resultats = matchRepository.rechercherPublics(
                LocalDateTime.now(), null, null);

        assertThat(resultats).extracting(Match::getId).contains(m.getId());
    }

    @Test
    @DisplayName("rechercherPublics avec siteId précisé filtre bien")
    void rechercherPublicsFiltreSite() {
        Match m = creer(LocalDateTime.now().plusDays(5),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);

        // Le terrain du test est sur le site 1 → siteId=1 doit le retourner
        List<Match> resultatsSite1 = matchRepository.rechercherPublics(
                LocalDateTime.now(), null, 1L);
        assertThat(resultatsSite1).extracting(Match::getId).contains(m.getId());

        // siteId=2 → ne doit pas retourner ce match
        List<Match> resultatsSite2 = matchRepository.rechercherPublics(
                LocalDateTime.now(), null, 2L);
        assertThat(resultatsSite2).extracting(Match::getId).doesNotContain(m.getId());
    }

    @Test
    @DisplayName("rechercherPublics retourne les résultats triés par date croissante")
    void rechercherPublicsTrieParDateAsc() {
        Match plusTard = creer(LocalDateTime.now().plusDays(10),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        Match plusTot = creer(LocalDateTime.now().plusDays(3),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);
        Match moyen = creer(LocalDateTime.now().plusDays(7),
                TypeMatch.PUBLIC, StatutMatch.PROGRAMME, false);

        List<Match> resultats = matchRepository.rechercherPublics(
                LocalDateTime.now(), null, null);

        // Filtre uniquement nos 3 matchs (la DB peut contenir d'autres données du seed)
        List<Match> nos3 = resultats.stream()
                .filter(m -> List.of(plusTot.getId(), moyen.getId(), plusTard.getId())
                        .contains(m.getId()))
                .toList();

        assertThat(nos3).extracting(Match::getId)
                .containsExactly(plusTot.getId(), moyen.getId(), plusTard.getId());
    }

}