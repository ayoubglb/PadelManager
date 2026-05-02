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
@DisplayName("Intégration Mes Transactions — GET /transactions")
class MesTransactionsIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private AuthService authService;

    private static final String EMAIL = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter() {
        return authService.connecter(new LoginRequest(EMAIL, PASSWORD)).token();
    }

    private void rechargerCompte(String montant) {
        Utilisateur u = utilisateurRepository.findByEmail(EMAIL).orElseThrow();
        Transaction recharge = Transaction.builder()
                .utilisateur(u).type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal(montant)).build();
        transactionRepository.save(recharge);
    }

    @Test
    @DisplayName("GET /transactions sans authentification → 401 ou 403")
    void transactionsSansAuth() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /transactions nominal → 200 + transactions du user")
    void transactionsNominal() throws Exception {
        rechargerCompte("75.00");
        String token = connecter();

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    @DisplayName("GET /transactions?type=RECHARGE → ne retourne que les RECHARGE")
    void transactionsFiltreType() throws Exception {
        rechargerCompte("50.00");
        String token = connecter();

        mockMvc.perform(get("/transactions")
                        .param("type", "RECHARGE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("RECHARGE"))));
    }
}