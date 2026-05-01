package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.inscription.InviterJoueurRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.TerrainRepository;
import be.ephec.padelmanager.repository.UtilisateurRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Invitation — POST /matchs/{id}/joueurs")
class InviterJoueurIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String EMAIL_ORGA = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    // Trouve un terrain actif sur le site de rattachement de l'organisateur
    private Terrain trouverTerrainAnderlecht() {
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_ORGA).orElseThrow();
        return terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(membre.getSiteRattachement().getId()))
                .filter(Terrain::getActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun terrain actif sur le site de rattachement"));
    }

    // Date dans X jours à 14h (cohérent avec MatchIntegrationTest)
    private LocalDateTime dateMatchDansJours(int jours) {
        return LocalDateTime.now().plusDays(jours)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    // Crée un match privé via l'API et retourne son id.
    // Le paramètre joursDansLeFutur évite les conflits de créneau entre tests.
    private Long creerMatchPrive(String token, int joursDansLeFutur) throws Exception {
        Terrain terrain = trouverTerrainAnderlecht();
        CreateMatchRequest req = new CreateMatchRequest(
                terrain.getId(), dateMatchDansJours(joursDansLeFutur), TypeMatch.PRIVE);
        String body = mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("POST /matchs/{id}/joueurs sans authentification → 401 ou 403")
    void inviterSansAuth() throws Exception {
        InviterJoueurRequest req = new InviterJoueurRequest("G100001");

        mockMvc.perform(post("/matchs/1/joueurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /matchs/{id}/joueurs nominal → 201 + InscriptionMatch persistée paye=false")
    void inviterNominal() throws Exception {
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 2);

        long inscritsAvant = inscriptionMatchRepository.findInscritsByMatchId(matchId).size();

        InviterJoueurRequest req = new InviterJoueurRequest("S200002");

        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.joueurMatricule").value("S200002"))
                .andExpect(jsonPath("$.paye").value(false))
                .andExpect(jsonPath("$.statut").value("INSCRIT"))
                .andExpect(jsonPath("$.estOrganisateur").value(false));

        long inscritsApres = inscriptionMatchRepository.findInscritsByMatchId(matchId).size();
        assertThat(inscritsApres).isEqualTo(inscritsAvant + 1);
    }

    @Test
    @DisplayName("POST /matchs/{id}/joueurs matricule format invalide → 400")
    void inviterMatriculeInvalide() throws Exception {
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(token, 4);

        String json = "{\"matricule\":\"invalid\"}";

        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/joueurs matricule inexistant → 400")
    void inviterMatriculeInexistant() throws Exception {
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(token, 6);

        InviterJoueurRequest req = new InviterJoueurRequest("L999999");

        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/joueurs par un membre qui n'est pas l'organisateur → 403")
    void inviterParAutreMembreRefuse() throws Exception {
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 8);

        // Autre membre du même site, mais pas l'organisateur du match
        String tokenAutre = connecter("membre.anderlecht.2@padelmanager.be", "Dev2026!");

        InviterJoueurRequest req = new InviterJoueurRequest("S200003");

        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + tokenAutre)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}