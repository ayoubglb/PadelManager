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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Reporting Site — GET /admin/reporting/sites/{siteId}")
class ReportingSiteIntegrationTest {

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
    @DisplayName("ADMIN_GLOBAL peut consulter le reporting de n'importe quel site → 200")
    void adminGlobalConsulteNimporteQuelSite() throws Exception {
        String token = connecter("admin.global@padelmanager.be", "Admin2026!");

        mockMvc.perform(get("/admin/reporting/sites/1")
                        .header("Authorization", "Bearer " + token)
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caEncaisse").exists());
    }

    @Test
    @DisplayName("ADMIN_SITE Anderlecht peut consulter SON site (id=1) → 200")
    void adminSiteConsulteSonSite() throws Exception {
        String token = connecter("admin.site.anderlecht@padelmanager.be", "Admin2026!");

        mockMvc.perform(get("/admin/reporting/sites/1")
                        .header("Authorization", "Bearer " + token)
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN_SITE Anderlecht ne peut PAS consulter Forest (id=2) → 403")
    void adminSiteNePeutConsulterAutreSite() throws Exception {
        String token = connecter("admin.site.anderlecht@padelmanager.be", "Admin2026!");

        mockMvc.perform(get("/admin/reporting/sites/2")
                        .header("Authorization", "Bearer " + token)
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-12-31"))
                .andExpect(status().isForbidden());
    }
}