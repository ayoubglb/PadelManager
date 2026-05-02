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
@DisplayName("Intégration Mes Matchs — GET /matchs/mes-matchs")
class MesMatchsIntegrationTest {

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

    private Long creerMatchPrive(String token, int joursDansLeFutur) throws Exception {
        Terrain terrain = trouverTerrainAnderlecht();
        LocalDateTime date = LocalDateTime.now().plusDays(joursDansLeFutur)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        CreateMatchRequest req = new CreateMatchRequest(terrain.getId(), date, TypeMatch.PRIVE);

        String body = mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("GET /matchs/mes-matchs sans authentification → 401 ou 403")
    void mesMatchsSansAuth() throws Exception {
        mockMvc.perform(get("/matchs/mes-matchs"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("GET /matchs/mes-matchs nominal → 200 + match créé apparaît avec monRole=ORGANISATEUR")
    void mesMatchsNominal() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(token, 12);

        mockMvc.perform(get("/matchs/mes-matchs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].monRole").value("ORGANISATEUR"))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].maPartPayee").value(true))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].siteNom").value("Anderlecht"));
    }

    @Test
    @DisplayName("GET /matchs/mes-matchs?aVenir=false → ne retourne pas les matchs futurs")
    void mesMatchsPassesNeContiennentPasFuturs() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(token, 13);

        mockMvc.perform(get("/matchs/mes-matchs")
                        .param("aVenir", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")]").doesNotExist());
    }
}