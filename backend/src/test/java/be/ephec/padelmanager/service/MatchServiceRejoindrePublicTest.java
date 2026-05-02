package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.dto.inscription.RejoindreMatchResponse;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.InscriptionMatchMapper;
import be.ephec.padelmanager.mapper.TransactionMapper;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de MatchService.rejoindreMatchPublic
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — rejoindre un match public")
class MatchServiceRejoindrePublicTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SoldeService soldeService;
    @Mock private InscriptionMatchMapper inscriptionMatchMapper;
    @Mock private TransactionMapper transactionMapper;

    // Mocks requis par le constructeur de MatchService mais non utilisés ici
    @Mock private be.ephec.padelmanager.repository.TerrainRepository terrainRepository;
    @Mock private be.ephec.padelmanager.repository.HoraireSiteRepository horaireSiteRepository;
    @Mock private be.ephec.padelmanager.repository.JourFermetureRepository jourFermetureRepository;
    @Mock private be.ephec.padelmanager.repository.UtilisateurRepository utilisateurRepository;
    @Mock private be.ephec.padelmanager.mapper.MatchMapper matchMapper;
    @Mock private java.time.Clock clock;

    @InjectMocks
    private MatchService matchService;

    private Utilisateur joueur, organisateur;
    private Match match;

    @BeforeEach
    void setUp() {
        organisateur = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        joueur = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        match = Match.builder()
                .id(50L).type(TypeMatch.PUBLIC).statut(StatutMatch.PROGRAMME)
                .organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        lenient().when(inscriptionMatchMapper.toDto(any(InscriptionMatch.class)))
                .thenReturn(new InscriptionMatchDTO(
                        1L, 50L, 200L, "L600002", "Maria Lopez",
                        LocalDateTime.now(), true, StatutInscription.INSCRIT, false));
        lenient().when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(
                        42L, 200L, TypeTransaction.PAIEMENT_MATCH,
                        PricingConstants.PART_JOUEUR, LocalDateTime.now(), 50L));
    }

    // ─── Cas nominal ────────────────────────────────────────────────────

    @Test
    @DisplayName("rejoindrePublic crée inscription paye=true + transaction PAIEMENT_MATCH 15€")
    void rejoindreNominal() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(false);
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(soldeService.disposeAuMoinsDe(200L, PricingConstants.PART_JOUEUR)).thenReturn(true);
        when(inscriptionMatchRepository.save(any(InscriptionMatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RejoindreMatchResponse reponse = matchService.rejoindreMatchPublic(50L, joueur);

        ArgumentCaptor<InscriptionMatch> inscCaptor = ArgumentCaptor.forClass(InscriptionMatch.class);
        verify(inscriptionMatchRepository).save(inscCaptor.capture());
        InscriptionMatch insc = inscCaptor.getValue();
        assertThat(insc.getMatch()).isEqualTo(match);
        assertThat(insc.getJoueur()).isEqualTo(joueur);
        assertThat(insc.getPaye()).isTrue();
        assertThat(insc.getStatut()).isEqualTo(StatutInscription.INSCRIT);
        assertThat(insc.getEstOrganisateur()).isFalse();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction tx = txCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(TypeTransaction.PAIEMENT_MATCH);
        assertThat(tx.getMontant()).isEqualByComparingTo(PricingConstants.PART_JOUEUR);
        assertThat(tx.getMatch()).isEqualTo(match);

        assertThat(reponse).isNotNull();
        assertThat(reponse.inscription()).isNotNull();
        assertThat(reponse.transaction()).isNotNull();
    }

    // ─── Validations ────────────────────────────────────────────────────

    @Test
    @DisplayName("rejoindrePublic refuse si match introuvable → EntityNotFoundException")
    void matchIntrouvable() {
        when(matchRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(999L, joueur))
                .isInstanceOf(EntityNotFoundException.class);

        verify(inscriptionMatchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si match PRIVE → IllegalArgumentException")
    void matchPasPublicRefuse() {
        match.setType(TypeMatch.PRIVE);
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publics");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si match ANNULE → IllegalArgumentException")
    void matchPasProgrammeRefuse() {
        match.setStatut(StatutMatch.ANNULE);
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ouvert");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si user est l'organisateur → IllegalArgumentException")
    void organisateurRefuse() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organisateur");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si déjà inscrit → IllegalArgumentException")
    void dejaInscritRefuse() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(true);

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà inscrit");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si match COMPLET (4 joueurs inscrits)")
    void matchCompletRefuse() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(false);
        // 4 inscriptions = match complet
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of(
                new InscriptionMatch(), new InscriptionMatch(),
                new InscriptionMatch(), new InscriptionMatch()));

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("complet");

        verify(inscriptionMatchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic refuse si solde insuffisant")
    void soldeInsuffisantRefuse() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(false);
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(soldeService.disposeAuMoinsDe(200L, PricingConstants.PART_JOUEUR)).thenReturn(false);

        assertThatThrownBy(() -> matchService.rejoindreMatchPublic(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solde insuffisant");

        verify(inscriptionMatchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejoindrePublic utilise findByIdForUpdate (verrou pessimiste)")
    void utiliseFindByIdForUpdate() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(false);
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(soldeService.disposeAuMoinsDe(any(), any())).thenReturn(true);
        when(inscriptionMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        matchService.rejoindreMatchPublic(50L, joueur);

        // Vérifie qu'on a bien appelé findByIdForUpdate (verrou) et pas findById classique
        verify(matchRepository).findByIdForUpdate(50L);
        verify(matchRepository, never()).findById(50L);
    }

}