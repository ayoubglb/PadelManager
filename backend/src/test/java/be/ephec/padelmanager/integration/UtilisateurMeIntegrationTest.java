package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Profil — GET /utilisateurs/me")
class UtilisateurMeIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String EMAIL_MEMBRE = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    @Test
    @DisplayName("GET /utilisateurs/me sans authentification → 401 ou 403")
    void meSansAuth() throws Exception {
        mockMvc.perform(get("/utilisateurs/me"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /utilisateurs/me nominal → 200 + profil sans données sensibles")
    void meNominal() throws Exception {
        String token = connecter(EMAIL_MEMBRE, PASSWORD);

        mockMvc.perform(get("/utilisateurs/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_MEMBRE))
                .andExpect(jsonPath("$.matricule").value("S200001"))
                .andExpect(jsonPath("$.role").value("MEMBRE_SITE"))
                .andExpect(jsonPath("$.siteRattachementNom").value("Anderlecht"))
                .andExpect(jsonPath("$.active").value(true))
                // Aucun mot de passe ou hash ne doit être exposé
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}