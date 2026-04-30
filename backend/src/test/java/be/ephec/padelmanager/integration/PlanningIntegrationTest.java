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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests d'intégration end-to-end GET /sites/{id}/planning sur SQL Server 2022 via Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Planning — GET /sites/{id}/planning")
class PlanningIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Connecte un compte seed et retourne le JWT
    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    @Test
    @DisplayName("GET /sites/1/planning sans authentification → 401 ou 403")
    void planningSansAuth() throws Exception {
        mockMvc.perform(get("/sites/1/planning").param("date", LocalDate.now().toString()))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /sites/1/planning avec membre authentifié → 200 et structure attendue")
    void planningNominal() throws Exception {
        String token = connecter("admin.global@padelmanager.be", "Admin2026!");
        LocalDate date = LocalDate.of(2026, 6, 15);

        mockMvc.perform(get("/sites/1/planning")
                        .header("Authorization", "Bearer " + token)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value(1))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.ferme").value(false))
                .andExpect(jsonPath("$.terrains").isArray())
                .andExpect(jsonPath("$.creneaux").isArray());
    }

    @Test
    @DisplayName("GET /sites/{autre}/planning par un MEMBRE_SITE rattaché ailleurs → 403")
    void planningMembreSiteAutreSiteRefuse() throws Exception {
        String token = connecter("membre.site@padelmanager.be", "Dev2026!");

        mockMvc.perform(get("/sites/2/planning")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-06-15"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /sites/{site_rattachement}/planning par un MEMBRE_SITE rattaché → 200")
    void planningMembreSiteSurSonSiteOk() throws Exception {
        String token = connecter("membre.site@padelmanager.be", "Dev2026!");

        mockMvc.perform(get("/sites/1/planning")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value(1));
    }

    @Test
    @DisplayName("GET /sites/999/planning sur site inexistant → 404")
    void planningSiteInexistant() throws Exception {
        String token = connecter("admin.global@padelmanager.be", "Admin2026!");

        mockMvc.perform(get("/sites/999/planning")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-06-15"))
                .andExpect(status().isNotFound());
    }
}