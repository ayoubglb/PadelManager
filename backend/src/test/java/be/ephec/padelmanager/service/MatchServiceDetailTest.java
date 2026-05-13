package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.dto.match.MatchDetailDTO;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.InscriptionMatchMapper;
import be.ephec.padelmanager.mapper.MatchMapper;
import be.ephec.padelmanager.mapper.TransactionMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService — consulterDetailMatch (EF-MB-013)")
class MatchServiceDetailTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private HoraireSiteRepository horaireSiteRepository;
    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private SoldeService soldeService;
    @Mock private MatchMapper matchMapper;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private InscriptionMatchMapper inscriptionMatchMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private PenaliteRepository penaliteRepository;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private MatchService matchService;

    private Site site;
    private Terrain terrain;
    private Utilisateur organisateur;
    private Utilisateur invite;
    private Utilisateur etranger;
    private Utilisateur adminGlobal;
    private Match matchPrive;
    private Match matchPublic;

    @BeforeEach
    void setUp() {
        site = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        terrain = Terrain.builder().id(10L).numero(1).site(site).active(true).build();

        organisateur = Utilisateur.builder()
                .id(100L).matricule("S200001").nom("Org").prenom("Anisateur")
                .role(RoleUtilisateur.MEMBRE_SITE).active(true).build();

        invite = Utilisateur.builder()
                .id(101L).matricule("S200002").nom("Inv").prenom("Ité")
                .role(RoleUtilisateur.MEMBRE_SITE).active(true).build();

        etranger = Utilisateur.builder()
                .id(102L).matricule("S200003").nom("Etr").prenom("Anger")
                .role(RoleUtilisateur.MEMBRE_SITE).active(true).build();

        adminGlobal = Utilisateur.builder()
                .id(999L).matricule("AG100001").nom("Admin").prenom("Global")
                .role(RoleUtilisateur.ADMIN_GLOBAL).active(true).build();

        matchPrive = Match.builder()
                .id(50L).terrain(terrain).organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.now(clock).plusDays(3))
                .dateHeureFin(LocalDateTime.now(clock).plusDays(3).plusMinutes(90))
                .type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .dateCreation(LocalDateTime.now(clock).minusDays(1))
                .build();

        matchPublic = Match.builder()
                .id(51L).terrain(terrain).organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.now(clock).plusDays(2))
                .dateHeureFin(LocalDateTime.now(clock).plusDays(2).plusMinutes(90))
                .type(TypeMatch.PUBLIC).statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .dateCreation(LocalDateTime.now(clock).minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Match introuvable → EntityNotFoundException")
    void matchInexistant() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.consulterDetailMatch(999L, organisateur))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Match introuvable");
    }

    @Test
    @DisplayName("Match PUBLIC accessible par n'importe quel authentifié")
    void matchPublicAccessibleParTous() {
        when(matchRepository.findById(51L)).thenReturn(Optional.of(matchPublic));
        when(inscriptionMatchRepository.findByMatchId(51L)).thenReturn(List.of());

        MatchDetailDTO detail = matchService.consulterDetailMatch(51L, etranger);

        assertThat(detail.id()).isEqualTo(51L);
        assertThat(detail.type()).isEqualTo(TypeMatch.PUBLIC);
    }

    @Test
    @DisplayName("Match PRIVE accessible par l'organisateur")
    void matchPriveAccessibleParOrganisateur() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(matchPrive));
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());

        MatchDetailDTO detail = matchService.consulterDetailMatch(50L, organisateur);

        assertThat(detail.id()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Match PRIVE accessible par un joueur inscrit")
    void matchPriveAccessibleParInvite() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(matchPrive));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 101L)).thenReturn(true);
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());

        MatchDetailDTO detail = matchService.consulterDetailMatch(50L, invite);

        assertThat(detail.id()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Match PRIVE refusé pour un étranger non admin → AccessDeniedException")
    void matchPriveRefuseEtranger() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(matchPrive));
        when(inscriptionMatchRepository.existsByMatchIdAndJoueurId(50L, 102L)).thenReturn(false);

        assertThatThrownBy(() -> matchService.consulterDetailMatch(50L, etranger))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("accès");
    }

    @Test
    @DisplayName("Match PRIVE accessible par ADMIN_GLOBAL même s'il n'est pas concerné")
    void matchPriveAccessibleParAdmin() {
        when(matchRepository.findById(50L)).thenReturn(Optional.of(matchPrive));
        when(inscriptionMatchRepository.findByMatchId(50L)).thenReturn(List.of());

        MatchDetailDTO detail = matchService.consulterDetailMatch(50L, adminGlobal);

        assertThat(detail.id()).isEqualTo(50L);
    }
}