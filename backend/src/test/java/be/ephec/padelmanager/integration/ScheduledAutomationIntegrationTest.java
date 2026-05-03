package be.ephec.padelmanager.integration;

import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.repository.TransactionRepository;
import be.ephec.padelmanager.repository.PenaliteRepository;
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
import java.math.BigDecimal;

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
    @Autowired private PenaliteRepository penaliteRepository;
    @Autowired private TransactionRepository transactionRepository;

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

    @Test
    @DisplayName("Convertit un match PRIVE incomplet en PUBLIC + crée pénalité 1 semaine")
    void convertirMatchPriveIncompletEnPublic() {
        Utilisateur orga = utilisateurRepository
                .findByEmail("membre.site@padelmanager.be").orElseThrow();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();

        // Match PRIVE dans 18h, sans inscription (donc incomplet)
        Match match = matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusHours(18))
                .dateHeureFin(LocalDateTime.now().plusHours(18).plusMinutes(90))
                .type(TypeMatch.PRIVE)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());

        // Exécute le job
        automationService.convertirPrivesIncomplets();

        // Match converti en PUBLIC
        Match matchApres = matchRepository.findById(match.getId()).orElseThrow();
        assertThat(matchApres.getType()).isEqualTo(TypeMatch.PUBLIC);
        assertThat(matchApres.getDevenuPublicAutomatiquement()).isTrue();

        // Pénalité créée pour CE match précisément (au lieu de compter le total)
        Penalite penalite = penaliteRepository.findAll().stream()
                .filter(p -> p.getMatch() != null && p.getMatch().getId().equals(match.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Aucune pénalité créée pour le match " + match.getId()));
        assertThat(penalite.getMotif()).isEqualTo("CONVERSION_AUTO_PRIVE_PUBLIC");
        assertThat(penalite.getUtilisateur().getId()).isEqualTo(orga.getId());
    }

    @Test
    @DisplayName("Ne convertit pas un match PUBLIC déjà existant (idempotence)")
    void publicNonReconverti() {
        Utilisateur orga = utilisateurRepository
                .findByEmail("membre.site@padelmanager.be").orElseThrow();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();

        // Match déjà PUBLIC dans 18h
        Match match = matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusHours(18))
                .dateHeureFin(LocalDateTime.now().plusHours(18).plusMinutes(90))
                .type(TypeMatch.PUBLIC)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());

        long penalitesAvant = penaliteRepository.count();

        automationService.convertirPrivesIncomplets();

        // Pas de pénalité créée (match déjà PUBLIC)
        assertThat(penaliteRepository.count()).isEqualTo(penalitesAvant);

        Match matchApres = matchRepository.findById(match.getId()).orElseThrow();
        assertThat(matchApres.getDevenuPublicAutomatiquement()).isFalse();  // Pas modifié
    }

    @Test
    @DisplayName("SYS : crée SOLDE_DU_ORGANISATEUR pour match PUBLIC incomplet")
    void facturerMatchPublicIncomplet() {
        Utilisateur orga = utilisateurRepository
                .findByEmail("membre.site@padelmanager.be").orElseThrow();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();

        // Match PUBLIC dans 18h, sans aucune inscription (4 places vides)
        Match match = matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusHours(18))
                .dateHeureFin(LocalDateTime.now().plusHours(18).plusMinutes(90))
                .type(TypeMatch.PUBLIC)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());

        // Exécute le job
        automationService.facturerOrganisateursPublicsIncomplets();

        // Vérifie : transaction SOLDE_DU_ORGANISATEUR créée pour ce match
        Transaction dette = transactionRepository.findAll().stream()
                .filter(t -> t.getMatch() != null && t.getMatch().getId().equals(match.getId()))
                .filter(t -> t.getType() == TypeTransaction.SOLDE_DU_ORGANISATEUR)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Aucune dette SOLDE_DU_ORGANISATEUR créée pour le match " + match.getId()));

        // 4 places vides × 15€ = 60€
        assertThat(dette.getMontant()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(dette.getUtilisateur().getId()).isEqualTo(orga.getId());
    }

    @Test
    @DisplayName("SYS idempotence : second appel ne crée pas de doublon")
    void facturerIdempotence() {
        Utilisateur orga = utilisateurRepository
                .findByEmail("membre.site@padelmanager.be").orElseThrow();
        Terrain terrain = terrainRepository.findAll().stream()
                .filter(t -> t.getSite().getId().equals(orga.getSiteRattachement().getId()))
                .findFirst().orElseThrow();

        Match match = matchRepository.save(Match.builder()
                .terrain(terrain)
                .organisateur(orga)
                .dateHeureDebut(LocalDateTime.now().plusHours(20))
                .dateHeureFin(LocalDateTime.now().plusHours(20).plusMinutes(90))
                .type(TypeMatch.PUBLIC)
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build());

        // 1er appel
        automationService.facturerOrganisateursPublicsIncomplets();
        long dettesApres1 = transactionRepository.findAll().stream()
                .filter(t -> t.getMatch() != null && t.getMatch().getId().equals(match.getId()))
                .filter(t -> t.getType() == TypeTransaction.SOLDE_DU_ORGANISATEUR)
                .count();
        assertThat(dettesApres1).isEqualTo(1);

        // 2ème appel : ne doit pas créer de doublon
        automationService.facturerOrganisateursPublicsIncomplets();
        long dettesApres2 = transactionRepository.findAll().stream()
                .filter(t -> t.getMatch() != null && t.getMatch().getId().equals(match.getId()))
                .filter(t -> t.getType() == TypeTransaction.SOLDE_DU_ORGANISATEUR)
                .count();
        assertThat(dettesApres2).isEqualTo(1);
    }
}