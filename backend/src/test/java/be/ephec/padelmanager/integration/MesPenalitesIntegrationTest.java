package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.PenaliteRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Pénalités — GET /utilisateurs/me/penalites + /active")
class MesPenalitesIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PenaliteRepository penaliteRepository;

    @Test
    @DisplayName("GET /utilisateurs/me/penalites sans authentification → 401 ou 403")
    void sansAuth() throws Exception {
        mockMvc.perform(get("/utilisateurs/me/penalites"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    @DisplayName("GET /utilisateurs/me/penalites/active sans pénalité → 200 avec body null")
    void aucunePenaliteActive() throws Exception {
        String token = authService.connecter(
                new LoginRequest("membre.libre@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/utilisateurs/me/penalites/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(""));  // body null = empty string
    }

    @Test
    @DisplayName("GET /utilisateurs/me/penalites/active avec pénalité active → 200 + DTO")
    void avecPenaliteActive() throws Exception {
        // Crée une pénalité pour le user
        Utilisateur user = utilisateurRepository
                .findByEmail("membre.libre@padelmanager.be").orElseThrow();
        penaliteRepository.save(Penalite.builder()
                .utilisateur(user)
                .dateDebut(LocalDateTime.now().minusDays(1))
                .dateFin(LocalDateTime.now().plusDays(6))
                .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                .build());

        String token = authService.connecter(
                new LoginRequest("membre.libre@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/utilisateurs/me/penalites/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motif").value("CONVERSION_AUTO_PRIVE_PUBLIC"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("GET /utilisateurs/me/penalites → 200 + liste de toutes les pénalités")
    void listeToutesLesPenalites() throws Exception {
        Utilisateur user = utilisateurRepository
                .findByEmail("membre.libre@padelmanager.be").orElseThrow();
        // Pénalité passée
        penaliteRepository.save(Penalite.builder()
                .utilisateur(user)
                .dateDebut(LocalDateTime.now().minusMonths(2))
                .dateFin(LocalDateTime.now().minusMonths(2).plusWeeks(1))
                .motif("ANCIENNE_PENALITE")
                .build());

        String token = authService.connecter(
                new LoginRequest("membre.libre@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/utilisateurs/me/penalites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].motif").value(org.hamcrest.Matchers.hasItem("ANCIENNE_PENALITE")));
    }
}