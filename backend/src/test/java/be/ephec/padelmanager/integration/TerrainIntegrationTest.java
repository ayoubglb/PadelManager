package be.ephec.padelmanager.integration;

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
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Intégration Terrain — comptage des seeds par site")
class TerrainIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /sites/1/terrains → 6 terrains pour Anderlecht (CF-RS-007)")
    void anderlechtA6Terrains() throws Exception {
        mockMvc.perform(get("/sites/1/terrains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("GET /sites/2/terrains → 5 terrains pour Forest")
    void forestA5Terrains() throws Exception {
        mockMvc.perform(get("/sites/2/terrains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("GET /sites/4/terrains → 4 terrains pour Sint-Pieters-Leeuw")
    void splA4Terrains() throws Exception {
        mockMvc.perform(get("/sites/4/terrains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }
}