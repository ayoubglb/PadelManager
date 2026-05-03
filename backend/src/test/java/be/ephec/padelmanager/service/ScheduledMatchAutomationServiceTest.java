package be.ephec.padelmanager.service;

import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.PenaliteRepository;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledMatchAutomationService — libération des places non payées")
class ScheduledMatchAutomationServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;
    @Mock private PenaliteRepository penaliteRepository;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private ScheduledMatchAutomationService service;

    private Match creerMatch(Long id, LocalDateTime debut) {
        return creerMatch(id, debut, TypeMatch.PRIVE);
    }

    private Match creerMatch(Long id, LocalDateTime debut, TypeMatch type) {
        Utilisateur orga = Utilisateur.builder()
                .id(100L).matricule("L600001").nom("Doe").prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();
        return Match.builder()
                .id(id).type(type).statut(StatutMatch.PROGRAMME)
                .dateHeureDebut(debut).dateHeureFin(debut.plusMinutes(90))
                .organisateur(orga)
                .build();
    }

    @Test
    @DisplayName("libererPlacesNonPayees aucun match à échéance → no-op")
    void aucunMatchAEcheance() {
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of());

        service.libererPlacesNonPayees();

        verify(inscriptionMatchRepository, never()).marquerLibereesNonPayees(anyLong());
    }

    @Test
    @DisplayName("libererPlacesNonPayees 1 match avec 2 inscriptions non payées → marquerLibereesNonPayees(50L) appelé")
    void unMatchAvecInscriptionsNonPayees() {
        Match match = creerMatch(50L, LocalDateTime.now(clock).plusHours(20));
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of(match));
        when(inscriptionMatchRepository.marquerLibereesNonPayees(50L)).thenReturn(2);

        service.libererPlacesNonPayees();

        verify(inscriptionMatchRepository).marquerLibereesNonPayees(50L);
    }

    @Test
    @DisplayName("libererPlacesNonPayees 3 matchs → marquerLibereesNonPayees appelé 3 fois")
    void plusieursMatchsAEcheance() {
        Match m1 = creerMatch(50L, LocalDateTime.now(clock).plusHours(20));
        Match m2 = creerMatch(51L, LocalDateTime.now(clock).plusHours(15));
        Match m3 = creerMatch(52L, LocalDateTime.now(clock).plusHours(5));
        when(matchRepository.findMatchsAEcheance24h(any(), any()))
                .thenReturn(List.of(m1, m2, m3));
        when(inscriptionMatchRepository.marquerLibereesNonPayees(anyLong())).thenReturn(1);

        service.libererPlacesNonPayees();

        verify(inscriptionMatchRepository, times(3)).marquerLibereesNonPayees(anyLong());
        verify(inscriptionMatchRepository).marquerLibereesNonPayees(50L);
        verify(inscriptionMatchRepository).marquerLibereesNonPayees(51L);
        verify(inscriptionMatchRepository).marquerLibereesNonPayees(52L);
    }

    @Test
    @DisplayName("libererPlacesNonPayees match sans inscription non payée → 0 libération")
    void matchSansInscriptionNonPayee() {
        Match match = creerMatch(50L, LocalDateTime.now(clock).plusHours(20));
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of(match));
        when(inscriptionMatchRepository.marquerLibereesNonPayees(50L)).thenReturn(0);

        service.libererPlacesNonPayees();

        verify(inscriptionMatchRepository).marquerLibereesNonPayees(50L);
    }

    // ─── SYS — convertirPrivesIncomplets ──────────

    @Test
    @DisplayName("convertirPrivesIncomplets aucun match à échéance → no-op")
    void convertirAucunMatch() {
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of());

        service.convertirPrivesIncomplets();

        verify(matchRepository, never()).save(any());
        verify(penaliteRepository, never()).save(any());
    }

    @Test
    @DisplayName("convertirPrivesIncomplets match PRIVE incomplet → conversion + pénalité")
    void convertirPriveIncomplet() {
        Match match = creerMatch(50L, LocalDateTime.now(clock).plusHours(20), TypeMatch.PRIVE);
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of(match));
        when(inscriptionMatchRepository.countJoueursPayesByMatchId(50L)).thenReturn(2L);

        service.convertirPrivesIncomplets();

        // Match converti en PUBLIC
        assertThat(match.getType()).isEqualTo(TypeMatch.PUBLIC);
        assertThat(match.getDevenuPublicAutomatiquement()).isTrue();
        verify(matchRepository).save(match);

        // Pénalité créée
        ArgumentCaptor<Penalite> penaliteCaptor = ArgumentCaptor.forClass(Penalite.class);
        verify(penaliteRepository).save(penaliteCaptor.capture());
        Penalite p = penaliteCaptor.getValue();
        assertThat(p.getMatch()).isEqualTo(match);
        assertThat(p.getMotif()).isEqualTo("CONVERSION_AUTO_PRIVE_PUBLIC");
        assertThat(p.getDateFin()).isEqualTo(p.getDateDebut().plusWeeks(1));
    }

    @Test
    @DisplayName("convertirPrivesIncomplets match PRIVE complet (4 payés) → pas de conversion")
    void privePasConvertiSiComplet() {
        Match match = creerMatch(50L, LocalDateTime.now(clock).plusHours(20), TypeMatch.PRIVE);
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of(match));
        when(inscriptionMatchRepository.countJoueursPayesByMatchId(50L)).thenReturn(4L);

        service.convertirPrivesIncomplets();

        assertThat(match.getType()).isEqualTo(TypeMatch.PRIVE);
        verify(matchRepository, never()).save(any());
        verify(penaliteRepository, never()).save(any());
    }

    @Test
    @DisplayName("convertirPrivesIncomplets match déjà PUBLIC → ignoré (idempotence)")
    void publicIgnore() {
        Match match = creerMatch(50L, LocalDateTime.now(clock).plusHours(20), TypeMatch.PUBLIC);
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of(match));

        service.convertirPrivesIncomplets();

        verify(inscriptionMatchRepository, never()).countJoueursPayesByMatchId(any());
        verify(matchRepository, never()).save(any());
        verify(penaliteRepository, never()).save(any());
    }

    @Test
    @DisplayName("executerJobsAutomatisation orchestre SYS-002 puis SYS-001")
    void orchestrateurAppelleSys002EtSys001() {
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of());

        service.executerJobsAutomatisation();

        // findMatchsAEcheance24h appelé 2 fois (1 pour SYS-002, 1 pour SYS-001)
        verify(matchRepository, times(2)).findMatchsAEcheance24h(any(), any());
    }
}