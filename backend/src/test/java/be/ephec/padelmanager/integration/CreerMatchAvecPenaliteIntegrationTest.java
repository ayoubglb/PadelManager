package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.PenaliteRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration — Création de match refusée si pénalité active")
class CreerMatchAvecPenaliteIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private PenaliteRepository penaliteRepository;
    @Autowired private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String EMAIL = "membre.site@padelmanager.be";
    private static final String PASSWORD = "Dev2026!";

    @Test
    @DisplayName("POST /matchs refuse si organisateur a une pénalité active → 400 + message clair")
    void creerMatchAvecPenaliteActive() throws Exception {
        // Recharge le compte (sinon erreur de solde insuffisant avant le check pénalité)
        Utilisateur user = utilisateurRepository.findByEmail(EMAIL).orElseThrow();
        transactionRepository.save(Transaction.builder()
                .utilisateur(user).type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal("100.00")).build());

        // Crée une pénalité active de 7 jours
        penaliteRepository.save(Penalite.builder()
                .utilisateur(user)
                .dateDebut(LocalDateTime.now().minusDays(1))
                .dateFin(LocalDateTime.now().plusDays(6))
                .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                .build());

        // Tente de créer un match
        String token = authService.connecter(new LoginRequest(EMAIL, PASSWORD)).token();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(user.getSiteRattachement().getId()))
                .filter(Terrain::getActive)
                .findFirst().orElseThrow();
        CreateMatchRequest req = new CreateMatchRequest(
                terrain.getId(),
                LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0),
                TypeMatch.PRIVE);

        mockMvc.perform(post("/matchs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("pénalité active")));
    }
}