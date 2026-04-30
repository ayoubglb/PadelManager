package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.transaction.RechargeRequest;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.TransactionRepository;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.service.SoldeService;
import be.ephec.padelmanager.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests d'intégration end-to-end POST /transactions/recharge sur SQL Server 2022 via Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Recharge — POST /transactions/recharge avec authentification et persistance")
class RechargeIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private SoldeService soldeService;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String EMAIL_MEMBRE_SITE = "membre.site@padelmanager.be";
    private static final String PASSWORD_MEMBRE_SITE = "Dev2026!";

    // Connecte le membre seed via le service réel et retourne le JWT.
    private String connecter() {
        var loginRequest = new LoginRequest(EMAIL_MEMBRE_SITE, PASSWORD_MEMBRE_SITE);
        return authService.connecter(loginRequest).token();
    }

    @Test
    @DisplayName("POST /transactions/recharge sans authentification → 401 ou 403")
    void rechargerSansAuth() throws Exception {
        RechargeRequest requete = new RechargeRequest(new BigDecimal("50.00"));

        mockMvc.perform(post("/transactions/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /transactions/recharge valide → 201 + Transaction RECHARGE persistée + solde augmenté")
    void rechargerNominal() throws Exception {
        String token = connecter();
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_MEMBRE_SITE).orElseThrow();

        long transactionsAvant = transactionRepository.count();
        BigDecimal soldeAvant = soldeService.calculerSolde(membre.getId());

        RechargeRequest requete = new RechargeRequest(new BigDecimal("75.50"));

        mockMvc.perform(post("/transactions/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.utilisateurId").value(membre.getId()))
                .andExpect(jsonPath("$.type").value("RECHARGE"))
                .andExpect(jsonPath("$.montant").value(75.50))
                .andExpect(jsonPath("$.matchId").doesNotExist())
                .andExpect(jsonPath("$.date").exists());

        assertThat(transactionRepository.count()).isEqualTo(transactionsAvant + 1);
        BigDecimal soldeApres = soldeService.calculerSolde(membre.getId());
        assertThat(soldeApres.subtract(soldeAvant)).isEqualByComparingTo("75.50");
    }

    @Test
    @DisplayName("POST /transactions/recharge avec montant null → 400")
    void rechargerMontantNull() throws Exception {
        String token = connecter();
        String jsonInvalide = "{\"montant\":null}";

        mockMvc.perform(post("/transactions/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalide))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/recharge avec montant zéro → 400")
    void rechargerMontantZero() throws Exception {
        String token = connecter();
        RechargeRequest requete = new RechargeRequest(BigDecimal.ZERO);

        mockMvc.perform(post("/transactions/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/recharge avec montant négatif → 400")
    void rechargerMontantNegatif() throws Exception {
        String token = connecter();
        RechargeRequest requete = new RechargeRequest(new BigDecimal("-10.00"));

        mockMvc.perform(post("/transactions/recharge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }
}