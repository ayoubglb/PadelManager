package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Rejoindre Public — POST /matchs/{id}/rejoindre")
class RejoindreMatchPublicIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;
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
    private Long creerMatchPublic(int joursDansLeFutur) throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Terrain terrain = trouverTerrainAnderlecht();
        LocalDateTime date = LocalDateTime.now().plusDays(joursDansLeFutur)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        CreateMatchRequest req = new CreateMatchRequest(terrain.getId(), date, TypeMatch.PUBLIC);

        String body = mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre sans authentification → 401 ou 403")
    void rejoindreSansAuth() throws Exception {
        mockMvc.perform(post("/matchs/1/rejoindre")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre nominal → 200 + inscription paye=true + transaction 15€")
    void rejoindreNominal() throws Exception {
        Long matchId = creerMatchPublic(2);
        rechargerCompte("membre.anderlecht.2@padelmanager.be", "30.00");
        String tokenJoueur = connecter("membre.anderlecht.2@padelmanager.be", PASSWORD);

        long inscritsAvant = inscriptionMatchRepository.findInscritsByMatchId(matchId).size();

        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenJoueur)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscription.matchId").value(matchId))
                .andExpect(jsonPath("$.inscription.joueurMatricule").value("S200002"))
                .andExpect(jsonPath("$.inscription.paye").value(true))
                .andExpect(jsonPath("$.inscription.estOrganisateur").value(false))
                .andExpect(jsonPath("$.transaction.type").value("PAIEMENT_MATCH"))
                .andExpect(jsonPath("$.transaction.montant").value(PricingConstants.PART_JOUEUR.doubleValue()));

        long inscritsApres = inscriptionMatchRepository.findInscritsByMatchId(matchId).size();
        assertThat(inscritsApres).isEqualTo(inscritsAvant + 1);
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre par l'organisateur → 400")
    void rejoindreParOrganisateurRefuse() throws Exception {
        Long matchId = creerMatchPublic(4);
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre deux fois → 400 (déjà inscrit)")
    void rejoindreDeuxFoisRefuse() throws Exception {
        Long matchId = creerMatchPublic(6);
        rechargerCompte("membre.anderlecht.3@padelmanager.be", "60.00");
        String tokenJoueur = connecter("membre.anderlecht.3@padelmanager.be", PASSWORD);

        // Premier rejoindre → succès
        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenJoueur)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Deuxième tentative → refus
        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenJoueur)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre avec solde insuffisant → 400")
    void rejoindreSoldeInsuffisantRefuse() throws Exception {
        Long matchId = creerMatchPublic(8);
        // S200005 (Forest, solde initial 0) tente de rejoindre
        String tokenForest = connecter("membre.forest.1@padelmanager.be", PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenForest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/rejoindre sur match complet → 400")
    void rejoindreMatchCompletRefuse() throws Exception {
        Long matchId = creerMatchPublic(10);

        // Remplit le match avec 3 autres joueurs (l'orga compte déjà comme 1)
        for (int i = 2; i <= 4; i++) {
            String email = "membre.anderlecht." + i + "@padelmanager.be";
            rechargerCompte(email, "30.00");
            String token = connecter(email, PASSWORD);
            mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Le 5e joueur tente → match complet
        rechargerCompte("membre.forest.1@padelmanager.be", "30.00");
        String tokenCinquieme = connecter("membre.forest.1@padelmanager.be", PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/rejoindre")
                        .header("Authorization", "Bearer " + tokenCinquieme)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}