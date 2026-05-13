package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.repository.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration GET /matchs/{id}")
class DetailMatchIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    private Match creerMatch(Utilisateur orga, TypeMatch type) {
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();
        return matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusDays(3))
                .dateHeureFin(LocalDateTime.now().plusDays(3).plusMinutes(90))
                .type(type)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());
    }

    @Test
    @DisplayName("GET /matchs/{id} sans authentification → 401 ou 403")
    void sansAuth() throws Exception {
        mockMvc.perform(get("/matchs/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    @DisplayName("GET /matchs/{id} match PUBLIC accessible par n'importe quel authentifié → 200")
    void matchPublicAccessibleParTous() throws Exception {
        Utilisateur orga = utilisateurRepository.findByEmail("membre.site@padelmanager.be").orElseThrow();
        Match match = creerMatch(orga, TypeMatch.PUBLIC);

        // Un membre d'un autre site (Forest) accède au match d'Anderlecht
        String token = authService.connecter(new LoginRequest(
                "membre.forest.1@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/matchs/" + match.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId()))
                .andExpect(jsonPath("$.type").value("PUBLIC"))
                .andExpect(jsonPath("$.inscriptions").isArray());
    }

    @Test
    @DisplayName("GET /matchs/{id} match PRIVE refusé pour un étranger → 403")
    void matchPriveRefuseEtranger() throws Exception {
        Utilisateur orga = utilisateurRepository.findByEmail("membre.site@padelmanager.be").orElseThrow();
        Match match = creerMatch(orga, TypeMatch.PRIVE);

        // Un membre d'un autre site qui n'a rien à voir avec ce match
        String token = authService.connecter(new LoginRequest(
                "membre.forest.1@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/matchs/" + match.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /matchs/{id} match PRIVE accessible par l'organisateur → 200")
    void matchPriveAccessibleParOrganisateur() throws Exception {
        Utilisateur orga = utilisateurRepository.findByEmail("membre.site@padelmanager.be").orElseThrow();
        Match match = creerMatch(orga, TypeMatch.PRIVE);

        String token = authService.connecter(new LoginRequest(
                "membre.site@padelmanager.be", "Dev2026!")).token();

        mockMvc.perform(get("/matchs/" + match.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId()))
                .andExpect(jsonPath("$.type").value("PRIVE"));
    }

    @Test
    @DisplayName("GET /matchs/{id} match PRIVE accessible par ADMIN_GLOBAL → 200")
    void matchPriveAccessibleParAdminGlobal() throws Exception {
        Utilisateur orga = utilisateurRepository.findByEmail("membre.site@padelmanager.be").orElseThrow();
        Match match = creerMatch(orga, TypeMatch.PRIVE);

        String token = authService.connecter(new LoginRequest(
                "admin.global@padelmanager.be", "Admin2026!")).token();

        mockMvc.perform(get("/matchs/" + match.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(match.getId()));
    }
}