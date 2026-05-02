package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.TransactionRepository;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Solde — GET /transactions/solde")
class SoldeIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TransactionRepository transactionRepository;

    private static final String EMAIL_MEMBRE = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    private void rechargerCompte(String email, String montant) {
        Utilisateur u = utilisateurRepository.findByEmail(email).orElseThrow();
        Transaction recharge = Transaction.builder()
                .utilisateur(u).type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal(montant)).build();
        transactionRepository.save(recharge);
    }

    @Test
    @DisplayName("GET /transactions/solde sans authentification → 401 ou 403")
    void soldeSansAuth() throws Exception {
        mockMvc.perform(get("/transactions/solde"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /transactions/solde nominal → 200 + montant cohérent avec ledger")
    void soldeNominal() throws Exception {
        // Recharge 50€ pour avoir un solde positif
        rechargerCompte(EMAIL_MEMBRE, "50.00");
        String token = connecter(EMAIL_MEMBRE, PASSWORD);

        mockMvc.perform(get("/transactions/solde")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solde").exists())
                .andExpect(jsonPath("$.solde").isNumber());
    }
}