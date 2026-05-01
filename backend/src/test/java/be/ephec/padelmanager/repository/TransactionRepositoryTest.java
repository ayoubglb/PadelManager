package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Tests d'intégration des requêtes custom de TransactionRepository sur SQL Server 2022
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("TransactionRepository — requêtes custom")
class TransactionRepositoryTest {

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
    @Autowired private TransactionRepository transactionRepository;

    private Utilisateur utilisateur;
    private Match match;

    @BeforeEach
    void setUp() {
        // On insère un utilisateur dédié au test pour ne pas dépendre du seed
        utilisateur = em.persistAndFlush(Utilisateur.builder()
                .matricule("L999001")
                .email("test.solde@padelmanager.be")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Test").prenom("Solde")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());

        // Match associé à un site/terrain seed (id=1 = premier site Anderlecht en seed)
        Site site = em.find(Site.class, 1L);
        Terrain terrain = em.getEntityManager()
                .createQuery("SELECT t FROM Terrain t WHERE t.site.id = 1 AND t.numero = 1", Terrain.class)
                .getSingleResult();

        match = em.persistAndFlush(Match.builder()
                .terrain(terrain)
                .dateHeureDebut(LocalDateTime.of(2026, 6, 15, 10, 0))
                .dateHeureFin(LocalDateTime.of(2026, 6, 15, 11, 30))
                .organisateur(utilisateur)
                .type(TypeMatch.PRIVE)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());
    }

    private Transaction creer(TypeTransaction type, String montant, Match m) {
        return em.persistAndFlush(Transaction.builder()
                .utilisateur(utilisateur)
                .type(type)
                .montant(new BigDecimal(montant))
                .match(m)
                .build());
    }

    // ─── calculerSoldeUtilisateur  ────────────────────────────

    @Test
    @DisplayName("calculerSoldeUtilisateur sans aucune transaction → 0")
    void soldeVidePourUtilisateurSansTransaction() {
        BigDecimal solde = transactionRepository.calculerSoldeUtilisateur(utilisateur.getId());

        assertThat(solde).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calculerSoldeUtilisateur additionne RECHARGE et REMBOURSEMENT (crédits)")
    void soldeCreditsRechargeEtRemboursement() {
        creer(TypeTransaction.RECHARGE, "100.00", null);
        creer(TypeTransaction.REMBOURSEMENT, "15.00", match);

        BigDecimal solde = transactionRepository.calculerSoldeUtilisateur(utilisateur.getId());

        assertThat(solde).isEqualByComparingTo("115.00");
    }

    @Test
    @DisplayName("calculerSoldeUtilisateur soustrait PAIEMENT_MATCH et SOLDE_DU_ORGANISATEUR (débits)")
    void soldeDebitsPaiementsEtSoldeDu() {
        creer(TypeTransaction.RECHARGE, "100.00", null);
        creer(TypeTransaction.PAIEMENT_MATCH, "15.00", match);
        creer(TypeTransaction.SOLDE_DU_ORGANISATEUR, "10.00", match);

        BigDecimal solde = transactionRepository.calculerSoldeUtilisateur(utilisateur.getId());

        // 100 - 15 - 10 = 75
        assertThat(solde).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("calculerSoldeUtilisateur isole bien les transactions par utilisateur")
    void soldeIsoleParUtilisateur() {
        Utilisateur autre = em.persistAndFlush(Utilisateur.builder()
                .matricule("L999002")
                .email("autre@padelmanager.be")
                .passwordHash("$2a$12$dummy.hash.for.testing.purposes.only.0123456")
                .nom("Autre").prenom("User")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build());

        creer(TypeTransaction.RECHARGE, "50.00", null);
        em.persistAndFlush(Transaction.builder()
                .utilisateur(autre)
                .type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal("999.00"))
                .build());

        BigDecimal solde = transactionRepository.calculerSoldeUtilisateur(utilisateur.getId());

        assertThat(solde).isEqualByComparingTo("50.00");
    }

    // ─── existsSoldeDuOrganisateurForMatch ────

    @Test
    @DisplayName("existsSoldeDuOrganisateurForMatch → false si aucun SOLDE_DU pour le match")
    void existsSoldeDuFauxSiAucun() {
        creer(TypeTransaction.PAIEMENT_MATCH, "15.00", match);

        boolean existe = transactionRepository.existsSoldeDuOrganisateurForMatch(match.getId());

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("existsSoldeDuOrganisateurForMatch → true si SOLDE_DU existe pour le match")
    void existsSoldeDuVraiSiPresent() {
        creer(TypeTransaction.SOLDE_DU_ORGANISATEUR, "10.00", match);

        boolean existe = transactionRepository.existsSoldeDuOrganisateurForMatch(match.getId());

        assertThat(existe).isTrue();
    }

    // ─── calculerCaBrut  ───────────────────────────

    @Test
    @DisplayName("calculerCaBrut additionne PAIEMENT_MATCH et SOLDE_DU dans la période")
    void caBrutDansPeriode() {
        creer(TypeTransaction.RECHARGE, "100.00", null);          // exclu
        creer(TypeTransaction.PAIEMENT_MATCH, "15.00", match);    // inclus
        creer(TypeTransaction.SOLDE_DU_ORGANISATEUR, "30.00", match); // inclus
        creer(TypeTransaction.REMBOURSEMENT, "5.00", match);      // exclu

        BigDecimal ca = transactionRepository.calculerCaBrut(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));

        assertThat(ca).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("calculerCaBrut hors période → 0")
    void caBrutHorsPeriode() {
        creer(TypeTransaction.PAIEMENT_MATCH, "15.00", match);

        BigDecimal ca = transactionRepository.calculerCaBrut(
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(20));

        assertThat(ca).isEqualByComparingTo("0");
    }

    // ─── calculerTotalRemboursements  ──────────────

    @Test
    @DisplayName("calculerTotalRemboursements somme uniquement les REMBOURSEMENT")
    void totalRemboursementsIsoleLeType() {
        creer(TypeTransaction.RECHARGE, "100.00", null);          // exclu
        creer(TypeTransaction.REMBOURSEMENT, "15.00", match);     // inclus
        creer(TypeTransaction.REMBOURSEMENT, "10.00", match);     // inclus
        creer(TypeTransaction.PAIEMENT_MATCH, "20.00", match);    // exclu

        BigDecimal total = transactionRepository.calculerTotalRemboursements(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));

        assertThat(total).isEqualByComparingTo("25.00");
    }
}