package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de MatchService.payerSaPart
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — paiement de la part d'un match privé")
class MatchServicePayerSaPartTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SoldeService soldeService;
    @Mock private TransactionMapper transactionMapper;

    // Mocks requis par le constructeur de MatchService mais non utilisés ici
    @Mock private be.ephec.padelmanager.repository.TerrainRepository terrainRepository;
    @Mock private be.ephec.padelmanager.repository.HoraireSiteRepository horaireSiteRepository;
    @Mock private be.ephec.padelmanager.repository.JourFermetureRepository jourFermetureRepository;
    @Mock private be.ephec.padelmanager.repository.UtilisateurRepository utilisateurRepository;
    @Mock private be.ephec.padelmanager.mapper.MatchMapper matchMapper;
    @Mock private be.ephec.padelmanager.mapper.InscriptionMatchMapper inscriptionMatchMapper;
    @Mock private java.time.Clock clock;

    @InjectMocks
    private MatchService matchService;

    private Utilisateur joueur, organisateur;
    private Match match;
    private InscriptionMatch inscriptionDuJoueur;

    @BeforeEach
    void setUp() {
        organisateur = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        joueur = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        match = Match.builder()
                .id(50L).type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        inscriptionDuJoueur = InscriptionMatch.builder()
                .id(1L).match(match).joueur(joueur)
                .paye(false).statut(StatutInscription.INSCRIT)
                .estOrganisateur(false).build();

        lenient().when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(
                        42L, 200L, TypeTransaction.PAIEMENT_MATCH,
                        PricingConstants.PART_JOUEUR, LocalDateTime.now(), 50L));
    }

    // ─── Cas nominal ────────────────────────────────────────────────────

    @Test
    @DisplayName("payerSaPart crée transaction PAIEMENT_MATCH 15€ + passe inscription.paye=true")
    void payerSaPartNominal() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchIdAndJoueurId(50L, 200L))
                .thenReturn(Optional.of(inscriptionDuJoueur));
        when(soldeService.disposeAuMoinsDe(200L, PricingConstants.PART_JOUEUR)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionMatchRepository.save(any(InscriptionMatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO resultat = matchService.payerSaPart(50L, joueur);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction tx = txCaptor.getValue();

        assertThat(tx.getType()).isEqualTo(TypeTransaction.PAIEMENT_MATCH);
        assertThat(tx.getMontant()).isEqualByComparingTo(PricingConstants.PART_JOUEUR);
        assertThat(tx.getUtilisateur()).isEqualTo(joueur);
        assertThat(tx.getMatch()).isEqualTo(match);

        ArgumentCaptor<InscriptionMatch> inscCaptor = ArgumentCaptor.forClass(InscriptionMatch.class);
        verify(inscriptionMatchRepository).save(inscCaptor.capture());
        assertThat(inscCaptor.getValue().getPaye()).isTrue();

        assertThat(resultat).isNotNull();
        assertThat(resultat.montant()).isEqualByComparingTo(PricingConstants.PART_JOUEUR);
    }

    // ─── Validations ────────────────────────────────────────────────────

    @Test
    @DisplayName("payerSaPart refuse si match introuvable → EntityNotFoundException")
    void matchIntrouvable() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.payerSaPart(999L, joueur))
                .isInstanceOf(EntityNotFoundException.class);

        verify(transactionRepository, never()).save(any());
        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si le match n'est pas PRIVE → IllegalArgumentException")
    void matchPasPriveRefuse() {
        match.setType(TypeMatch.PUBLIC);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.payerSaPart(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("privé");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si le match n'est pas PROGRAMME → IllegalArgumentException")
    void matchPasProgrammeRefuse() {
        match.setStatut(StatutMatch.ANNULE);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.payerSaPart(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("programmé");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si le joueur n'a pas d'inscription pour ce match")
    void joueurNonInscritRefuse() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchIdAndJoueurId(50L, 200L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.payerSaPart(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inscrit");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si l'organisateur tente de payer (déjà payé à la création)")
    void organisateurNePayePasRefuse() {
        InscriptionMatch inscriptionOrga = InscriptionMatch.builder()
                .id(2L).match(match).joueur(organisateur)
                .paye(true).statut(StatutInscription.INSCRIT)
                .estOrganisateur(true).build();

        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchIdAndJoueurId(50L, 100L))
                .thenReturn(Optional.of(inscriptionOrga));

        assertThatThrownBy(() -> matchService.payerSaPart(50L, organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organisateur");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si déjà payé → IllegalArgumentException")
    void dejaPayeRefuse() {
        inscriptionDuJoueur.setPaye(true);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchIdAndJoueurId(50L, 200L))
                .thenReturn(Optional.of(inscriptionDuJoueur));

        assertThatThrownBy(() -> matchService.payerSaPart(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà payé");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payerSaPart refuse si solde insuffisant → IllegalArgumentException")
    void soldeInsuffisantRefuse() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchIdAndJoueurId(50L, 200L))
                .thenReturn(Optional.of(inscriptionDuJoueur));
        when(soldeService.disposeAuMoinsDe(200L, PricingConstants.PART_JOUEUR)).thenReturn(false);

        assertThatThrownBy(() -> matchService.payerSaPart(50L, joueur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solde insuffisant");

        verify(transactionRepository, never()).save(any());
        verify(inscriptionMatchRepository, never()).save(any());
    }
}