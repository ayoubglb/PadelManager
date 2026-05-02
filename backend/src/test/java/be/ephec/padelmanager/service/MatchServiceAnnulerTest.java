package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.match.AnnulationMatchResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de MatchService.annulerMatch
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — annulation d'un match")
class MatchServiceAnnulerTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;

    // Mocks requis par le constructeur de MatchService mais non utilisés ici
    @Mock private be.ephec.padelmanager.repository.TerrainRepository terrainRepository;
    @Mock private be.ephec.padelmanager.repository.HoraireSiteRepository horaireSiteRepository;
    @Mock private be.ephec.padelmanager.repository.JourFermetureRepository jourFermetureRepository;
    @Mock private be.ephec.padelmanager.repository.UtilisateurRepository utilisateurRepository;
    @Mock private SoldeService soldeService;
    @Mock private be.ephec.padelmanager.mapper.MatchMapper matchMapper;
    @Mock private be.ephec.padelmanager.mapper.InscriptionMatchMapper inscriptionMatchMapper;

    // Clock figé au 1er juin 2026 à 10h. Le match de référence est le 5 juin à 14h.
    // → 4 jours de marge, donc bien > 48h pour les tests nominaux.
    private final Clock clockFige = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("UTC"));

    private MatchService matchService;

    private Utilisateur organisateur, autreUser;
    private Match match;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository, inscriptionMatchRepository, transactionRepository,
                terrainRepository, horaireSiteRepository, jourFermetureRepository,
                soldeService, matchMapper, clockFige,
                utilisateurRepository, inscriptionMatchMapper, transactionMapper);

        organisateur = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        autreUser = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        match = Match.builder()
                .id(50L).type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.of(2026, 6, 5, 14, 0))
                .build();

        lenient().when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(
                        42L, 200L, TypeTransaction.REMBOURSEMENT,
                        PricingConstants.PART_JOUEUR, LocalDateTime.now(), 50L));
    }

    private InscriptionMatch creerInscription(Long id, Utilisateur joueur, boolean paye, boolean estOrga) {
        return InscriptionMatch.builder()
                .id(id).match(match).joueur(joueur)
                .paye(paye).statut(StatutInscription.INSCRIT)
                .estOrganisateur(estOrga).build();
    }

    // ─── Cas nominaux ──────────────────────────────────────────────

    @Test
    @DisplayName("annulerMatch nominal → match passe ANNULE + remboursements créés pour les payants")
    void annulerMatchNominal() {
        // 3 joueurs payés (organisateur + 2 invités) → 3 remboursements attendus
        InscriptionMatch insc1 = creerInscription(1L, organisateur, true, true);
        InscriptionMatch insc2 = creerInscription(2L, autreUser, true, false);
        InscriptionMatch insc3 = creerInscription(3L,
                Utilisateur.builder().id(300L).build(), false, false);  // non payé → pas de remboursement

        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchId(50L))
                .thenReturn(List.of(insc1, insc2, insc3));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        AnnulationMatchResponse reponse = matchService.annulerMatch(50L, organisateur);

        // 2 remboursements (insc1 et insc2 payés, insc3 non payé)
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        assertThat(reponse.nombreRemboursements()).isEqualTo(2);
        assertThat(reponse.matchId()).isEqualTo(50L);

        // Match passe ANNULE
        assertThat(match.getStatut()).isEqualTo(StatutMatch.ANNULE);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("annulerMatch sans inscription payée → 0 remboursement, match passe ANNULE")
    void annulerMatchSansInscriptionPayee() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        AnnulationMatchResponse reponse = matchService.annulerMatch(50L, organisateur);

        assertThat(reponse.nombreRemboursements()).isEqualTo(0);
        assertThat(match.getStatut()).isEqualTo(StatutMatch.ANNULE);
        verify(transactionRepository, never()).save(any());
    }

    // ─── Validations ──────────────────────────────────────────────

    @Test
    @DisplayName("annulerMatch refuse si match introuvable → EntityNotFoundException")
    void matchIntrouvable() {
        when(matchRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.annulerMatch(999L, organisateur))
                .isInstanceOf(EntityNotFoundException.class);

        verify(matchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerMatch refuse si match déjà ANNULE → IllegalArgumentException")
    void matchDejaAnnuleRefuse() {
        match.setStatut(StatutMatch.ANNULE);
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.annulerMatch(50L, organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("programmé");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerMatch refuse si user n'est pas l'organisateur → AccessDeniedException")
    void utilisateurNonOrganisateurRefuse() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.annulerMatch(50L, autreUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("organisateur");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerMatch refuse match privé < 48h avant → IllegalArgumentException")
    void delaiPriveTropCourtRefuse() {
        // Match privé dans 24h seulement (au lieu de 48h min) à partir du clock figé
        match.setDateHeureDebut(LocalDateTime.of(2026, 6, 2, 10, 0));  // J+1 à partir du 1/6 10h
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.annulerMatch(50L, organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("48h");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerMatch refuse match public < 24h avant → IllegalArgumentException")
    void delaiPublicTropCourtRefuse() {
        match.setType(TypeMatch.PUBLIC);
        // Match public dans 12h (au lieu de 24h min)
        match.setDateHeureDebut(LocalDateTime.of(2026, 6, 1, 22, 0));  // 12h plus tard
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.annulerMatch(50L, organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24h");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annulerMatch accepte match public à 25h avant (juste au-dessus de la limite)")
    void delaiPublicJusteSuffisant() {
        match.setType(TypeMatch.PUBLIC);
        // Match public dans 25h → autorisé (25h > 24h)
        match.setDateHeureDebut(LocalDateTime.of(2026, 6, 2, 11, 0));
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        // Aucune exception
        AnnulationMatchResponse reponse = matchService.annulerMatch(50L, organisateur);
        assertThat(reponse).isNotNull();
        assertThat(match.getStatut()).isEqualTo(StatutMatch.ANNULE);
    }

    @Test
    @DisplayName("annulerMatch utilise findByIdForUpdate (verrou pessimiste)")
    void utiliseFindByIdForUpdate() {
        when(matchRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        matchService.annulerMatch(50L, organisateur);

        verify(matchRepository).findByIdForUpdate(50L);
        verify(matchRepository, never()).findById(50L);
    }
}