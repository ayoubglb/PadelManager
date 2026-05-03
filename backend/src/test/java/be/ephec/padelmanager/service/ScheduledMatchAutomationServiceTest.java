package be.ephec.padelmanager.service;

import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledMatchAutomationService — libération des places non payées")
class ScheduledMatchAutomationServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private InscriptionMatchRepository inscriptionMatchRepository;

    @Spy
    private Clock clock = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private ScheduledMatchAutomationService service;

    private Match creerMatch(Long id, LocalDateTime debut) {
        return Match.builder()
                .id(id).type(TypeMatch.PRIVE).statut(StatutMatch.PROGRAMME)
                .dateHeureDebut(debut).dateHeureFin(debut.plusMinutes(90))
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

    @Test
    @DisplayName("executerJobsAutomatisation orchestre l'appel à libererPlacesNonPayees")
    void orchestrateurAppelleSys002() {
        when(matchRepository.findMatchsAEcheance24h(any(), any())).thenReturn(List.of());

        service.executerJobsAutomatisation();

        verify(matchRepository).findMatchsAEcheance24h(any(), any());
    }
}