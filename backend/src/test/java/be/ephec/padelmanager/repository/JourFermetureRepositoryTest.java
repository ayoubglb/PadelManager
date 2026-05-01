package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.JourFermeture;
import be.ephec.padelmanager.entity.Site;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Tests d'intégration des requêtes custom de JourFermetureRepository sur SQL Server 2022
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("JourFermetureRepository — requêtes custom")
class JourFermetureRepositoryTest {

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mssql::getJdbcUrl);
        registry.add("spring.datasource.username", mssql::getUsername);
        registry.add("spring.datasource.password", mssql::getPassword);
    }

    @Autowired private TestEntityManager em;
    @Autowired private JourFermetureRepository jourFermetureRepository;

    private JourFermeture creerGlobale(LocalDate date, String raison) {
        return em.persistAndFlush(JourFermeture.builder()
                .dateFermeture(date).site(null).raison(raison).build());
    }

    private JourFermeture creerSurSite(LocalDate date, Long siteId, String raison) {
        Site site = em.find(Site.class, siteId);
        return em.persistAndFlush(JourFermeture.builder()
                .dateFermeture(date).site(site).raison(raison).build());
    }

    // ─── findGlobaleParDate ──────────────────────────────────────────────

    @Test
    @DisplayName("findGlobaleParDate retrouve une fermeture globale uniquement")
    void findGlobaleParDateGlobale() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        JourFermeture noel = creerGlobale(date, "Noël test");

        Optional<JourFermeture> trouve = jourFermetureRepository.findGlobaleParDate(date);

        assertThat(trouve).isPresent();
        assertThat(trouve.get().getId()).isEqualTo(noel.getId());
        assertThat(trouve.get().getSite()).isNull();
    }

    @Test
    @DisplayName("findGlobaleParDate ignore les fermetures site-spécifiques")
    void findGlobaleParDateIgnoreSpecifiques() {
        LocalDate date = LocalDate.of(2026, 7, 14);
        creerSurSite(date, 1L, "Travaux Anderlecht");

        Optional<JourFermeture> trouve = jourFermetureRepository.findGlobaleParDate(date);

        assertThat(trouve).isEmpty();
    }

    // ─── findParSiteEtDate ───────────────────────────────────────────────

    @Test
    @DisplayName("findParSiteEtDate retrouve une fermeture site-spécifique")
    void findParSiteEtDateExistante() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        JourFermeture jf = creerSurSite(date, 1L, "Maintenance Anderlecht");

        Optional<JourFermeture> trouve = jourFermetureRepository.findParSiteEtDate(1L, date);

        assertThat(trouve).isPresent();
        assertThat(trouve.get().getId()).isEqualTo(jf.getId());
    }

    @Test
    @DisplayName("findParSiteEtDate retourne empty pour un autre site à la même date")
    void findParSiteEtDateIsoleParSite() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        creerSurSite(date, 1L, "Site 1");

        Optional<JourFermeture> trouve = jourFermetureRepository.findParSiteEtDate(2L, date);

        assertThat(trouve).isEmpty();
    }

    // ─── estFermeAUneDate (utilisée par MatchService) ───────────────────

    @Test
    @DisplayName("estFermeAUneDate true si fermeture globale à la date")
    void estFermeAUneDateGlobaleVrai() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        creerGlobale(date, "Toussaint test");

        assertThat(jourFermetureRepository.estFermeAUneDate(1L, date)).isTrue();
        assertThat(jourFermetureRepository.estFermeAUneDate(2L, date)).isTrue();
    }

    @Test
    @DisplayName("estFermeAUneDate true uniquement pour le site concerné")
    void estFermeAUneDateSpecifiqueIsole() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        creerSurSite(date, 1L, "Site 1 ferme");

        assertThat(jourFermetureRepository.estFermeAUneDate(1L, date)).isTrue();
        assertThat(jourFermetureRepository.estFermeAUneDate(2L, date)).isFalse();
    }

    @Test
    @DisplayName("estFermeAUneDate false si aucune fermeture")
    void estFermeAUneDateAucune() {
        LocalDate date = LocalDate.of(2026, 3, 15);

        assertThat(jourFermetureRepository.estFermeAUneDate(1L, date)).isFalse();
    }
}