package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.RoleUtilisateur;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("PenaliteRepository — requêtes custom")
class PenaliteRepositoryTest {

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mssql::getJdbcUrl);
        registry.add("spring.datasource.username", mssql::getUsername);
        registry.add("spring.datasource.password", mssql::getPassword);
        registry.add("spring.liquibase.url",      mssql::getJdbcUrl);
        registry.add("spring.liquibase.user",     mssql::getUsername);
        registry.add("spring.liquibase.password", mssql::getPassword);
    }

    @Autowired private TestEntityManager em;
    @Autowired private PenaliteRepository penaliteRepository;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = em.persistAndFlush(Utilisateur.builder()
                .matricule("L999300")
                .email("test.penalite@padelmanager.be")
                .telephone("0000000000")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Test").prenom("Penalite")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("findActiveByUtilisateurId trouve une pénalité active (maintenant ∈ [debut, fin])")
    void trouvePenaliteActive() {
        LocalDateTime maintenant = LocalDateTime.now();
        em.persistAndFlush(Penalite.builder()
                .utilisateur(utilisateur)
                .dateDebut(maintenant.minusDays(2))
                .dateFin(maintenant.plusDays(5))
                .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                .build());

        Optional<Penalite> resultat = penaliteRepository
                .findActiveByUtilisateurId(utilisateur.getId(), maintenant);

        assertThat(resultat).isPresent();
        assertThat(resultat.get().getMotif()).isEqualTo("CONVERSION_AUTO_PRIVE_PUBLIC");
    }

    @Test
    @DisplayName("findActiveByUtilisateurId ne trouve pas une pénalité passée (fin avant maintenant)")
    void ignoreLesPenalitesPassees() {
        LocalDateTime maintenant = LocalDateTime.now();
        em.persistAndFlush(Penalite.builder()
                .utilisateur(utilisateur)
                .dateDebut(maintenant.minusDays(10))
                .dateFin(maintenant.minusDays(3))  // Fin il y a 3 jours
                .motif("ANCIENNE_PENALITE")
                .build());

        Optional<Penalite> resultat = penaliteRepository
                .findActiveByUtilisateurId(utilisateur.getId(), maintenant);

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("findActiveByUtilisateurId ne trouve pas une pénalité future (debut après maintenant)")
    void ignoreLesPenalitesFutures() {
        LocalDateTime maintenant = LocalDateTime.now();
        em.persistAndFlush(Penalite.builder()
                .utilisateur(utilisateur)
                .dateDebut(maintenant.plusDays(2))  // Commence dans 2 jours
                .dateFin(maintenant.plusDays(9))
                .motif("FUTURE_PENALITE")
                .build());

        Optional<Penalite> resultat = penaliteRepository
                .findActiveByUtilisateurId(utilisateur.getId(), maintenant);

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("findActiveByUtilisateurId ne retourne rien pour un utilisateur sans pénalité")
    void aucunePenaliteSiUtilisateurClean() {
        Optional<Penalite> resultat = penaliteRepository
                .findActiveByUtilisateurId(utilisateur.getId(), LocalDateTime.now());

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("UK partial sur match_id empêche d'insérer 2 pénalités pour le même match")
    void ukMatchEmpecheDoublons() {
        // Setup : crée un match
        be.ephec.padelmanager.entity.Terrain terrain = em.getEntityManager()
                .createQuery("SELECT t FROM Terrain t WHERE t.site.id = 1 AND t.numero = 1",
                        be.ephec.padelmanager.entity.Terrain.class)
                .getSingleResult();
        be.ephec.padelmanager.entity.Match match = em.persistAndFlush(
                be.ephec.padelmanager.entity.Match.builder()
                        .terrain(terrain)
                        .organisateur(utilisateur)
                        .dateHeureDebut(LocalDateTime.of(2026, 8, 1, 10, 0))
                        .dateHeureFin(LocalDateTime.of(2026, 8, 1, 11, 30))
                        .type(be.ephec.padelmanager.entity.TypeMatch.PRIVE)
                        .statut(be.ephec.padelmanager.entity.StatutMatch.PROGRAMME)
                        .devenuPublicAutomatiquement(false)
                        .build());

        // 1ère pénalité : OK
        em.persistAndFlush(Penalite.builder()
                .utilisateur(utilisateur)
                .dateDebut(LocalDateTime.now())
                .dateFin(LocalDateTime.now().plusWeeks(1))
                .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                .match(match)
                .build());

        // 2ème pénalité pour le même match : doit échouer (UK partial)
        Penalite doublon = Penalite.builder()
                .utilisateur(utilisateur)
                .dateDebut(LocalDateTime.now())
                .dateFin(LocalDateTime.now().plusWeeks(1))
                .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                .match(match)
                .build();

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> em.persistAndFlush(doublon))
                .isInstanceOfAny(
                        org.springframework.dao.DataIntegrityViolationException.class,
                        jakarta.persistence.PersistenceException.class)
                .hasMessageContaining("uk_penalite_match");
    }
}