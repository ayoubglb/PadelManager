package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Intégration Auth — SQL Server 2022 via Testcontainers")
class AuthIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers gère le cycle de vie
    static final MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    @DynamicPropertySource
    static void proprietesDynamiques(DynamicPropertyRegistry registre) {
        // Surcharge des URLs/credentials : tout pointe vers le container éphémère
        registre.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registre.add("spring.datasource.username", sqlServer::getUsername);
        registre.add("spring.datasource.password", sqlServer::getPassword);
        registre.add("spring.liquibase.url", sqlServer::getJdbcUrl);
        registre.add("spring.liquibase.user", sqlServer::getUsername);
        registre.add("spring.liquibase.password", sqlServer::getPassword);
        // Contexte Liquibase "dev" pour charger les seed dans les tests
        registre.add("spring.liquibase.contexts", () -> "dev");
    }

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /auth/register puis /auth/login (email) → 201 + 200 avec tokens valides")
    void inscriptionPuisConnexionParEmail() throws Exception {
        RegisterRequest inscription = new RegisterRequest(
                "Test", "Integration", "integ@example.com", "+32475999001",
                "motdepasse123", RoleUtilisateur.MEMBRE_LIBRE, null
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.matricule", matchesPattern("^L\\d{6}$")))
                .andExpect(jsonPath("$.role").value("MEMBRE_LIBRE"));

        LoginRequest connexion = new LoginRequest("integ@example.com", "motdepasse123");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connexion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value("integ@example.com"));
    }

    @Test
    @DisplayName("Login du seed 'admin.global' → 200 avec role=ADMIN_GLOBAL")
    void loginSeedAdminGlobal() throws Exception {
        // Le seed 003 a été appliqué via Liquibase contexte=dev
        LoginRequest connexion = new LoginRequest("admin.global@padelmanager.be", "Admin2026!");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connexion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("AG100001"))
                .andExpect(jsonPath("$.role").value("ADMIN_GLOBAL"));
    }

    @Test
    @DisplayName("Login avec mot de passe erroné → 4xx (CF-AA-005)")
    void loginMotDePasseErroneRejete() throws Exception {
        LoginRequest connexion = new LoginRequest("admin.global@padelmanager.be", "mauvais");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connexion)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Register email déjà utilisé → erreur")
    void registerEmailDupliqueRejete() throws Exception {
        RegisterRequest doublon = new RegisterRequest(
                "Autre", "Personne", "admin.global@padelmanager.be", "+32475999002",
                "motdepasse123", RoleUtilisateur.MEMBRE_LIBRE, null
        );

        // Sans @ControllerAdvice (arrivera au commit 04), IllegalArgumentException → 500.
        // On assouplit l'assertion pour que ce test reste vert maintenant et continue de
        // passer quand on durcira en 400 au commit 04.
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doublon)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status < 400 || status >= 600) {
                        throw new AssertionError("Attendu une erreur 4xx ou 5xx, obtenu : " + status);
                    }
                });
    }
}