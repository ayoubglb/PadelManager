package be.ephec.padelmanager.repository;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Tests d'intégration des requêtes custom de UtilisateurRepository sur SQL Server 2022

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("UtilisateurRepository — requêtes custom")
class UtilisateurRepositoryTest {

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
    @Autowired private UtilisateurRepository utilisateurRepository;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = em.persistAndFlush(Utilisateur.builder()
                .matricule("L999100")
                .email("repo.test@padelmanager.be")
                .telephone("0000000000")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Repo").prenom("Test")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("findByEmailOrMatricule retrouve un utilisateur par son email")
    void findByEmailOrMatriculeAvecEmail() {
        Optional<Utilisateur> trouve = utilisateurRepository
                .findByEmailOrMatricule("repo.test@padelmanager.be");

        assertThat(trouve).isPresent();
        assertThat(trouve.get().getId()).isEqualTo(utilisateur.getId());
    }

    @Test
    @DisplayName("findByEmailOrMatricule retrouve un utilisateur par son matricule")
    void findByEmailOrMatriculeAvecMatricule() {
        Optional<Utilisateur> trouve = utilisateurRepository
                .findByEmailOrMatricule("L999100");

        assertThat(trouve).isPresent();
        assertThat(trouve.get().getId()).isEqualTo(utilisateur.getId());
    }

    @Test
    @DisplayName("findByEmailOrMatricule retourne empty si aucun match")
    void findByEmailOrMatriculeAucunMatch() {
        Optional<Utilisateur> trouve = utilisateurRepository
                .findByEmailOrMatricule("inexistant@nowhere.be");

        assertThat(trouve).isEmpty();
    }
}