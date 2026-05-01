package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.inscription.InviterJoueurRequest;
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
@DisplayName("Intégration Paiement — POST /matchs/{id}/payer")
class PayerSaPartIntegrationTest {

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
    private static final String EMAIL_INVITE = "membre.anderlecht.2@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    private String connecter(String email, String password) {
        return authService.connecter(new LoginRequest(email, password)).token();
    }

    // Recharge le compte d'un utilisateur via une transaction RECHARGE persistée directement
    private void rechargerCompte(String email, String montant) {
        Utilisateur u = utilisateurRepository.findByEmail(email).orElseThrow();
        Transaction recharge = Transaction.builder()
                .utilisateur(u)
                .type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal(montant))
                .build();
        transactionRepository.save(recharge);
    }

    private Terrain trouverTerrainAnderlecht() {
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_ORGA).orElseThrow();
        return terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(membre.getSiteRattachement().getId()))
                .filter(Terrain::getActive)
                .findFirst()
                .orElseThrow();
    }

    private LocalDateTime dateMatchDansJours(int jours) {
        return LocalDateTime.now().plusDays(jours)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    // Crée un match privé via l'API + invite S200002. Retourne l'id du match
    private Long creerMatchPriveAvecInvite(int joursDansLeFutur) throws Exception {
        rechargerCompte(EMAIL_ORGA, "100.00");
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        Terrain terrain = trouverTerrainAnderlecht();

        CreateMatchRequest req = new CreateMatchRequest(
                terrain.getId(), dateMatchDansJours(joursDansLeFutur), TypeMatch.PRIVE);
        String body = mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long matchId = objectMapper.readTree(body).get("id").asLong();

        // Invite S200002 dans le match
        InviterJoueurRequest inviteReq = new InviterJoueurRequest("S200002");
        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteReq)))
                .andExpect(status().isCreated());

        return matchId;
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer sans authentification → 401 ou 403")
    void payerSansAuth() throws Exception {
        mockMvc.perform(post("/matchs/1/payer")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer nominal → 200 + transaction PAIEMENT_MATCH 15€ + inscription.paye=true")
    void payerNominal() throws Exception {
        Long matchId = creerMatchPriveAvecInvite(2);
        rechargerCompte(EMAIL_INVITE, "30.00");
        String tokenInvite = connecter(EMAIL_INVITE, PASSWORD);

        long transactionsAvant = transactionRepository.count();

        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenInvite)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId))
                .andExpect(jsonPath("$.type").value("PAIEMENT_MATCH"))
                .andExpect(jsonPath("$.montant").value(PricingConstants.PART_JOUEUR.doubleValue()));

        // +1 transaction PAIEMENT_MATCH créée
        assertThat(transactionRepository.count()).isEqualTo(transactionsAvant + 1);

        // Inscription marquée comme payée
        Utilisateur invite = utilisateurRepository.findByEmail(EMAIL_INVITE).orElseThrow();
        var inscription = inscriptionMatchRepository
                .findByMatchIdAndJoueurId(matchId, invite.getId()).orElseThrow();
        assertThat(inscription.getPaye()).isTrue();
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer par un user non inscrit → 400")
    void payerNonInscritRefuse() throws Exception {
        Long matchId = creerMatchPriveAvecInvite(4);
        // S200003 n'a PAS été invité, donc pas inscrit
        rechargerCompte("membre.anderlecht.3@padelmanager.be", "30.00");
        String tokenAutre = connecter("membre.anderlecht.3@padelmanager.be", PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenAutre)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer par l'organisateur (déjà payé) → 400")
    void payerOrganisateurRefuse() throws Exception {
        Long matchId = creerMatchPriveAvecInvite(6);
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer deux fois → 400 (déjà payé)")
    void payerDeuxFoisRefuse() throws Exception {
        Long matchId = creerMatchPriveAvecInvite(8);
        rechargerCompte(EMAIL_INVITE, "60.00");
        String tokenInvite = connecter(EMAIL_INVITE, PASSWORD);

        // Premier paiement → succès
        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenInvite)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Deuxième tentative → refus
        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenInvite)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs/{id}/payer avec solde insuffisant → 400")
    void payerSoldeInsuffisantRefuse() throws Exception {
        Long matchId = creerMatchPriveAvecInvite(10);

        // On invite S200005 (Forest, solde 0)
        String tokenOrga = connecter(EMAIL_ORGA, PASSWORD);
        InviterJoueurRequest inviteForest = new InviterJoueurRequest("S200005");
        mockMvc.perform(post("/matchs/" + matchId + "/joueurs")
                        .header("Authorization", "Bearer " + tokenOrga)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteForest)))
                .andExpect(status().isCreated());

        // S200005 (Forest, solde 0) tente de payer
        String tokenForest = connecter("membre.forest.1@padelmanager.be", PASSWORD);

        mockMvc.perform(post("/matchs/" + matchId + "/payer")
                        .header("Authorization", "Bearer " + tokenForest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}