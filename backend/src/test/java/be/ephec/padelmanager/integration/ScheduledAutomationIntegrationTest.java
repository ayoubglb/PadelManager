package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.TerrainRepository;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.service.ScheduledMatchAutomationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Intégration Job SYS — Libération des places non payées")
class ScheduledAutomationIntegrationTest {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired private ScheduledMatchAutomationService automationService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private InscriptionMatchRepository inscriptionMatchRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private TerrainRepository terrainRepository;

    @Test
    @DisplayName("Marque les inscriptions non payées en LIBERE_NON_PAIEMENT")
    void libereInscriptionsNonPayeesDansFenetre24h() {
        Utilisateur orga = utilisateurRepository
                .findByEmail("membre.site@padelmanager.be").orElseThrow();
        Utilisateur invite = utilisateurRepository
                .findByEmail("membre.anderlecht.2@padelmanager.be").orElseThrow();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();

        // Match dans 18h (dans la fenêtre 24h)
        Match match = matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusHours(18))
                .dateHeureFin(LocalDateTime.now().plusHours(18).plusMinutes(90))
                .type(TypeMatch.PRIVE)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());

        // Inscription organisateur (payée) - reste INSCRIT
        inscriptionMatchRepository.save(InscriptionMatch.builder()
                .match(match).joueur(orga)
                .paye(true).statut(StatutInscription.INSCRIT)
                .estOrganisateur(true).build());

        // Inscription invité (NON payée) - doit passer LIBERE_NON_PAIEMENT
        inscriptionMatchRepository.save(InscriptionMatch.builder()
                .match(match).joueur(invite)
                .paye(false).statut(StatutInscription.INSCRIT)
                .estOrganisateur(false).build());

        // Exécute le job
        automationService.libererPlacesNonPayees();

        List<InscriptionMatch> apres = inscriptionMatchRepository.findByMatchId(match.getId());
        assertThat(apres).hasSize(2);  // pas de DELETE, juste marquage

        InscriptionMatch orgaApres = apres.stream()
                .filter(i -> i.getJoueur().getId().equals(orga.getId()))
                .findFirst().orElseThrow();
        InscriptionMatch inviteApres = apres.stream()
                .filter(i -> i.getJoueur().getId().equals(invite.getId()))
                .findFirst().orElseThrow();

        assertThat(orgaApres.getStatut()).isEqualTo(StatutInscription.INSCRIT);
        assertThat(inviteApres.getStatut()).isEqualTo(StatutInscription.LIBERE_NON_PAIEMENT);
    }
}