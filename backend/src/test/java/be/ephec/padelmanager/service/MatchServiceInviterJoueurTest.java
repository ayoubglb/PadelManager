package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.StatutInscription;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.InscriptionMatchMapper;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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

// Tests unitaires Mockito de MatchService.inviterJoueur
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — invitation à un match privé")
class MatchServiceInviterJoueurTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private InscriptionMatchMapper inscriptionMatchMapper;

    // Mocks requis par le constructeur de MatchService mais non utilisés ici
    @Mock private be.ephec.padelmanager.repository.TerrainRepository terrainRepository;
    @Mock private be.ephec.padelmanager.repository.HoraireSiteRepository horaireSiteRepository;
    @Mock private be.ephec.padelmanager.repository.JourFermetureRepository jourFermetureRepository;
    @Mock private be.ephec.padelmanager.repository.TransactionRepository transactionRepository;
    @Mock private SoldeService soldeService;
    @Mock private be.ephec.padelmanager.mapper.MatchMapper matchMapper;
    @Mock private java.time.Clock clock;

    @InjectMocks
    private MatchService matchService;

    private Utilisateur organisateur, joueurInvite;
    private Match match;

    @BeforeEach
    void setUp() {
        organisateur = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        joueurInvite = Utilisateur.builder()
                .id(200L).matricule("L600002").nom("Lopez").prenom("Maria")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        match = Match.builder()
                .id(50L).type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        lenient().when(inscriptionMatchMapper.toDto(any(InscriptionMatch.class)))
                .thenReturn(new InscriptionMatchDTO(
                        1L, 50L, 200L, "L600002", "Maria Lopez",
                        LocalDateTime.now(), false, StatutInscription.INSCRIT, false));
    }

    // ─── Cas nominal ────────────────────────────────────────────────────

    @Test
    @DisplayName("inviterJoueur crée une InscriptionMatch paye=false statut=INSCRIT")
    void inviterJoueurCreeInscription() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(utilisateurRepository.findByMatricule("L600002")).thenReturn(Optional.of(joueurInvite));
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(false);
        when(inscriptionMatchRepository.save(any(InscriptionMatch.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InscriptionMatchDTO resultat = matchService.inviterJoueur(50L, "L600002", organisateur);

        ArgumentCaptor<InscriptionMatch> captor = ArgumentCaptor.forClass(InscriptionMatch.class);
        verify(inscriptionMatchRepository).save(captor.capture());
        InscriptionMatch sauvee = captor.getValue();

        assertThat(sauvee.getMatch()).isEqualTo(match);
        assertThat(sauvee.getJoueur()).isEqualTo(joueurInvite);
        assertThat(sauvee.getPaye()).isFalse();
        assertThat(sauvee.getStatut()).isEqualTo(StatutInscription.INSCRIT);
        assertThat(sauvee.getEstOrganisateur()).isFalse();
        assertThat(resultat).isNotNull();
    }

    // ─── Validations ────────────────────────────────────────────────────

    @Test
    @DisplayName("inviterJoueur refuse si match introuvable → EntityNotFoundException")
    void matchIntrouvable() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.inviterJoueur(999L, "L600002", organisateur))
                .isInstanceOf(EntityNotFoundException.class);

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si l'utilisateur n'est pas l'organisateur → AccessDeniedException")
    void pasOrganisateurRefuse() {
        Utilisateur autreUtilisateur = Utilisateur.builder()
                .id(999L).matricule("L600099").nom("Other").prenom("User")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", autreUtilisateur))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("organisateur");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si le match n'est pas PRIVE → IllegalArgumentException")
    void matchPasPriveRefuse() {
        match.setType(TypeMatch.PUBLIC);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("privé");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si le match n'est pas PROGRAMME → IllegalArgumentException")
    void matchPasProgrammeRefuse() {
        match.setStatut(StatutMatch.ANNULE);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("programmé");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si le match a déjà 4 inscrits")
    void matchCompletRefuse() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        InscriptionMatch insc = InscriptionMatch.builder().build();
        when(inscriptionMatchRepository.findInscritsByMatchId(50L))
                .thenReturn(List.of(insc, insc, insc, insc));

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("complet");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si matricule inconnu → IllegalArgumentException")
    void matriculeInconnuRefuse() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(utilisateurRepository.findByMatricule("L999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L999999", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matricule");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si joueur désactivé → IllegalArgumentException")
    void joueurDesactiveRefuse() {
        joueurInvite.setActive(false);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(utilisateurRepository.findByMatricule("L600002")).thenReturn(Optional.of(joueurInvite));

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("désactivé");

        verify(inscriptionMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("inviterJoueur refuse si joueur déjà inscrit → IllegalArgumentException")
    void joueurDejaInscritRefuse() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(inscriptionMatchRepository.findInscritsByMatchId(50L)).thenReturn(List.of());
        when(utilisateurRepository.findByMatricule("L600002")).thenReturn(Optional.of(joueurInvite));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 200L)).thenReturn(true);

        assertThatThrownBy(() -> matchService.inviterJoueur(50L, "L600002", organisateur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà inscrit");

        verify(inscriptionMatchRepository, never()).save(any());
    }
}