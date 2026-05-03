package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Reporting Global — GET /admin/reporting/global")
class ReportingGlobalIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    @Test
    @DisplayName("GET /admin/reporting/global sans authentification → 401 ou 403")
    void reportingGlobalSansAuth() throws Exception {
        mockMvc.perform(get("/admin/reporting/global")
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /admin/reporting/global par un membre → 403 Forbidden")
    void reportingGlobalParMembreRefuse() throws Exception {
        String token = connecter("membre.site@padelmanager.be", "Dev2026!");

        mockMvc.perform(get("/admin/reporting/global")
                        .header("Authorization", "Bearer " + token)
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/reporting/global par ADMIN_GLOBAL → 200 + reporting JSON")
    void reportingGlobalNominal() throws Exception {
        String token = connecter("admin.global@padelmanager.be", "Admin2026!");

        mockMvc.perform(get("/admin/reporting/global")
                        .header("Authorization", "Bearer " + token)
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caEncaisse").exists())
                .andExpect(jsonPath("$.volumeMatchs").exists())
                .andExpect(jsonPath("$.nombreMatchsTotaux").exists())
                .andExpect(jsonPath("$.nombreMatchsPrives").exists())
                .andExpect(jsonPath("$.nombreMatchsPublics").exists())
                .andExpect(jsonPath("$.nombreMatchsAnnules").exists())
                .andExpect(jsonPath("$.topOrganisateurs").isArray());
    }
}