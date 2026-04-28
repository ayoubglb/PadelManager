package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.repository.*;
import be.ephec.padelmanager.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Match — création complète avec authentification, validation et persistance")
class MatchIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private MatchRepository matchRepository;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final String EMAIL_MEMBRE_SITE = "membre.site@padelmanager.be";
    private static final String PASSWORD_MEMBRE_SITE = "Dev2026!";


    private String obtenirTokenAvecCompteCharge() {
        Utilisateur membre = utilisateurRepository
                .findByEmail(EMAIL_MEMBRE_SITE)
                .orElseThrow(() -> new IllegalStateException(
                        "Compte seed introuvable : " + EMAIL_MEMBRE_SITE));

        // Recharge 50 € pour avoir de quoi réserver plusieurs fois
        Transaction recharge = Transaction.builder()
                .utilisateur(membre)
                .type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal("50.00"))
                .build();
        transactionRepository.save(recharge);

        // Authentification via le service réel
        var loginRequest = new LoginRequest(EMAIL_MEMBRE_SITE, PASSWORD_MEMBRE_SITE);
        return authService.connecter(loginRequest).token();
    }


    private Terrain trouverTerrainAnderlecht() {
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_MEMBRE_SITE).orElseThrow();
        return terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(membre.getSiteRattachement().getId()))
                .filter(Terrain::getActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun terrain actif sur le site de rattachement"));
    }


    private LocalDateTime dateMatchDansJours(int jours) {
        return LocalDateTime.now().plusDays(jours)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("POST /matchs sans authentification → 401 ou 403")
    void creerMatchSansAuth() throws Exception {
        CreateMatchRequest requete = new CreateMatchRequest(
                1L, dateMatchDansJours(2), TypeMatch.PRIVE);

        mockMvc.perform(post("/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("POST /matchs valide → 201 Created + Match + InscriptionMatch + Transaction persistés")
    void creerMatchNominal() throws Exception {
        String token = obtenirTokenAvecCompteCharge();
        Utilisateur membre = utilisateurRepository.findByEmail(EMAIL_MEMBRE_SITE).orElseThrow();
        Terrain terrain = trouverTerrainAnderlecht();
        LocalDateTime dateMatch = dateMatchDansJours(2);

        CreateMatchRequest requete = new CreateMatchRequest(
                terrain.getId(), dateMatch, TypeMatch.PRIVE);

        long matchsAvant = matchRepository.count();
        long inscriptionsAvant = inscriptionMatchRepository.count();
        // Recharge déjà comptée → on s'attend à +1 transaction (PAIEMENT_MATCH)
        long transactionsAvant = transactionRepository.count();

        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.terrainId").value(terrain.getId()))
                .andExpect(jsonPath("$.terrainNumero").value(terrain.getNumero()))
                .andExpect(jsonPath("$.siteNom").value("Anderlecht"))
                .andExpect(jsonPath("$.type").value("PRIVE"))
                .andExpect(jsonPath("$.statut").value("PROGRAMME"))
                .andExpect(jsonPath("$.organisateurId").value(membre.getId()))
                .andExpect(jsonPath("$.devenuPublicAutomatiquement").value(false))
                .andExpect(jsonPath("$.termine").value(false));

        // Vérifie l'atomicité de la création
        assertThat(matchRepository.count()).isEqualTo(matchsAvant + 1);
        assertThat(inscriptionMatchRepository.count()).isEqualTo(inscriptionsAvant + 1);
        assertThat(transactionRepository.count()).isEqualTo(transactionsAvant + 1);
    }

    @Test
    @DisplayName("POST /matchs avec date passée → 400 (validation Bean @Future)")
    void creerMatchAvecDatePassee() throws Exception {
        String token = obtenirTokenAvecCompteCharge();

        CreateMatchRequest requete = new CreateMatchRequest(
                1L, LocalDateTime.now().minusDays(1), TypeMatch.PRIVE);

        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs sur créneau déjà pris → 400")
    void creerMatchSurCreneauPris() throws Exception {
        String token = obtenirTokenAvecCompteCharge();
        Terrain terrain = trouverTerrainAnderlecht();
        LocalDateTime dateMatch = dateMatchDansJours(3);

        CreateMatchRequest requete = new CreateMatchRequest(
                terrain.getId(), dateMatch, TypeMatch.PRIVE);

        // Première création → succès
        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated());

        // Deuxième tentative sur le même terrain au même créneau → rejet
        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matchs au-delà du délai 14j (MEMBRE_SITE)")
    void creerMatchAuDelaDuDelaiMembreSite() throws Exception {
        String token = obtenirTokenAvecCompteCharge();
        Terrain terrain = trouverTerrainAnderlecht();

        // 15 jours = au-delà du délai max de 14j pour MEMBRE_SITE
        CreateMatchRequest requete = new CreateMatchRequest(
                terrain.getId(), dateMatchDansJours(15), TypeMatch.PRIVE);

        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }
}