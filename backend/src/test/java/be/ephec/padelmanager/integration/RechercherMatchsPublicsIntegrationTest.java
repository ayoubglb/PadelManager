package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.TerrainRepository;
import be.ephec.padelmanager.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Catalogue — GET /matchs/publics")
class RechercherMatchsPublicsIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String EMAIL_ORGA = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    private void rechargerCompte(String email, String montant) {
        Utilisateur u = utilisateurRepository.findByEmail(email).orElseThrow();
        Transaction recharge = Transaction.builder()
                .utilisateur(u).type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal(montant)).build();
        transactionRepository.save(recharge);
    }

    private Terrain trouverTerrainAnderlecht() {
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_ORGA).orElseThrow();
        return terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(membre.getSiteRattachement().getId()))
                .filter(Terrain::getActive)
                .findFirst().orElseThrow();
    }

    // Crée un match PUBLIC via l'API et retourne son id
    private Long creerMatchPublic(String token, int joursDansLeFutur) throws Exception {
        Terrain terrain = trouverTerrainAnderlecht();
        LocalDateTime date = LocalDateTime.now().plusDays(joursDansLeFutur)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        CreateMatchRequest req = new CreateMatchRequest(terrain.getId(), date, TypeMatch.PUBLIC);

        String body = mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("GET /matchs/publics sans authentification → 401 ou 403")
    void catalogueSansAuth() throws Exception {
        mockMvc.perform(get("/matchs/publics"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /matchs/publics nominal → 200 + liste avec champs enrichis")
    void catalogueNominal() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPublic(token, 2);

        mockMvc.perform(get("/matchs/publics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].siteNom").value("Anderlecht"))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].placesRestantes").value(3))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].organisateurNom").exists());
    }

    @Test
    @DisplayName("GET /matchs/publics?siteId=1 filtre par site")
    void catalogueFiltreParSite() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPublic(token, 4);

        mockMvc.perform(get("/matchs/publics")
                        .param("siteId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")]").exists());

        // Filtre sur un autre site → ne doit pas retourner ce match
        mockMvc.perform(get("/matchs/publics")
                        .param("siteId", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /matchs/publics?placesMin=4 retourne les matchs à 4 places restantes uniquement")
    void catalogueFiltrePlacesMin() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPublic(token, 6);

        // Le match créé a 1 joueur (organisateur), donc 3 places restantes
        // placesMin=4 doit l'exclure
        mockMvc.perform(get("/matchs/publics")
                        .param("placesMin", "4")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")]").doesNotExist());

        // placesMin=3 doit l'inclure (3 places restantes ≥ 3)
        mockMvc.perform(get("/matchs/publics")
                        .param("placesMin", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")]").exists());
    }
}