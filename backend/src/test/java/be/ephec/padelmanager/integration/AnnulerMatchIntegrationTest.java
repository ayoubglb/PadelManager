package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.inscription.InviterJoueurRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.MatchRepository;
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
@DisplayName("Intégration Annulation — POST /matchs/{id}/annuler")
class AnnulerMatchIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private MatchRepository matchRepository;
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

    // Crée un match privé via l'API et retourne son id
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
    @DisplayName("POST /matchs/{id}/annuler sans authentification → 401 ou 403")
    void annulerSansAuth() throws Exception {
        mockMvc.perform(post("/matchs/1/annuler")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /matchs/{id}/annuler nominal → 200 + match ANNULE + remboursement organisateur")
    void annulerNominal() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 5);  // 5 jours dans le futur, > 48h

        long transactionsAvant = transactionRepository.count();

        mockMvc.perform(post("/matchs/" + matchId + "/annuler")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.nombreRemboursements").value(1))  // organisateur seul
                .andExpect(jsonPath("$.remboursements[0].type").value("REMBOURSEMENT"))
                .andExpect(jsonPath("$.remboursements[0].montant")
                        .value(PricingConstants.PART_JOUEUR.doubleValue()));

        // +1 transaction REMBOURSEMENT créée
        assertThat(transactionRepository.count()).isEqualTo(transactionsAvant + 1);

        // Match passe ANNULE
        var matchAnnule = matchRepository.findById(matchId).orElseThrow();
        assertThat(matchAnnule.getStatut()).isEqualTo(StatutMatch.ANNULE);
    }

    @Test
    @DisplayName("POST /matchs/{id}/annuler avec joueur invité payant → 2 remboursements")
    void annulerAvecJoueurPayant() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 7);  // 7 jours dans le futur

        // Inviter S200002 et lui faire payer
        InviterJoueurRequest inviteReq = new InviterJoueurRequest("S200002");
        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteReq)))
                .andExpect(status().isCreated());

        rechargerCompte("membre.anderlecht.2@padelmanager.be", "30.00");
        String tokenInvite = connecter("membre.anderlecht.2@padelmanager.be", PASSWORD);
        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenInvite)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // L'organisateur annule → 2 remboursements (orga + invité)
        mockMvc.perform(post("/matchs/" + matchId + "/annuler")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreRemboursements").value(2));
    }

    @Test
    @DisplayName("POST /matchs/{id}/annuler par un autre que l'organisateur → 403")
    void annulerParAutreUserRefuse() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 5);

        // Un autre membre tente d'annuler le match
        String tokenAutre = connecter("membre.anderlecht.2@padelmanager.be", PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/annuler")
                        .header("Authorization", "Bearer " + tokenAutre)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /matchs/{id}/annuler deux fois → 400 (déjà annulé)")
    void annulerDeuxFoisRefuse() throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Long matchId = creerMatchPrive(tokenOrga, 5);

        // Première annulation → succès
        mockMvc.perform(post("/matchs/" + matchId + "/annuler")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Deuxième tentative → refus
        mockMvc.perform(post("/matchs/" + matchId + "/annuler")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/annuler match introuvable → 404")
    void annulerMatchIntrouvable() throws Exception {
        rechargerCompte(EMAIL_ORGA, "30.00");
        String token = connecter(EMAIL_ORGA, PASSWORD);

        mockMvc.perform(post("/matchs/999999/annuler")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}