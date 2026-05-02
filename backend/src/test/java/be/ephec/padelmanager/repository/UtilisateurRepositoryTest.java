package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Site;
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

    // ─── findByIdWithSite (JOIN FETCH pour endpoint /me) ─────────────────

    @Test
    @DisplayName("findByIdWithSite charge le siteRattachement en eager (évite LazyInitializationException)")
    void findByIdWithSiteChargeLeSite() {
        Site site = em.persist(Site.builder()
                .nom("SiteTestFindByIdWithSite")  // ← nom unique pour ne pas conflicter avec le seed
                .adresse("Rue du Test 1")
                .codePostal("1070")
                .ville("Anderlecht")
                .active(true)
                .build());

        Utilisateur u = em.persist(Utilisateur.builder()
                .matricule("S200099").nom("Test").prenom("User")
                .email("test.findbyidwithsite@padelmanager.be")
                .telephone("0000000000")
                .passwordHash("hash")
                .role(RoleUtilisateur.MEMBRE_SITE)
                .siteRattachement(site)
                .active(true)
                .build());

        em.flush();
        em.clear();

        Utilisateur trouve = utilisateurRepository.findByIdWithSite(u.getId()).orElseThrow();

        assertThat(trouve.getSiteRattachement()).isNotNull();
        assertThat(trouve.getSiteRattachement().getNom()).isEqualTo("SiteTestFindByIdWithSite");
    }

    @Test
    @DisplayName("findByIdWithSite renvoie Optional.empty pour un id inconnu")
    void findByIdWithSiteIdInconnu() {
        Optional<Utilisateur> resultat = utilisateurRepository.findByIdWithSite(999999L);
        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("findByIdWithSite fonctionne aussi pour un utilisateur sans site rattachement (LEFT JOIN)")
    void findByIdWithSiteSansSite() {
        Utilisateur u = em.persist(Utilisateur.builder()
                .matricule("L600099").nom("Test").prenom("Libre")
                .email("test.libre@padelmanager.be")
                .telephone("0000000000")
                .passwordHash("hash")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .siteRattachement(null)  // Membre libre = pas de site
                .active(true)
                .build());

        em.flush();
        em.clear();

        Utilisateur trouve = utilisateurRepository.findByIdWithSite(u.getId()).orElseThrow();

        assertThat(trouve.getSiteRattachement()).isNull();
        assertThat(trouve.getMatricule()).isEqualTo("L600099");
    }


}