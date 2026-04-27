package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.site.CreateSiteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Intégration Site — endpoints publics + écriture admin")
class SiteIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /sites est public et retourne les 4 sites seedés")
    void getSitesEstPublic() throws Exception {
        mockMvc.perform(get("/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.nom == 'Anderlecht')]").exists())
                .andExpect(jsonPath("$[?(@.nom == 'Forest')]").exists())
                .andExpect(jsonPath("$[?(@.nom == 'Drogenbos')]").exists())
                .andExpect(jsonPath("$[?(@.nom == 'Sint-Pieters-Leeuw')]").exists());
    }

    @Test
    @DisplayName("POST /sites sans authentification → 403 avec corps d'erreur")
    void postSiteSansAuthRefuse() throws Exception {
        CreateSiteRequest req = new CreateSiteRequest("NewSite", "Rue X", "1000", "Bruxelles");
        mockMvc.perform(post("/sites")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /sites/admin sans authentification → 403 Forbidden")
    void getSitesAdminRefuseAnonyme() throws Exception {
        mockMvc.perform(get("/sites/admin"))
                .andExpect(status().isForbidden());
    }
}