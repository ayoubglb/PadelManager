package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.reporting.ReportingDTO;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportingService — reporting CA et stats admin")
class ReportingServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MatchRepository matchRepository;

    @InjectMocks
    private ReportingService reportingService;

    private void stubAllSummariesToZero() {
        lenient().when(transactionRepository.sommerParTypeEtPeriode(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(transactionRepository.sommerParTypesEtPeriode(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(matchRepository.compterMatchs(any(), any(), any(), any())).thenReturn(0L);
        lenient().when(matchRepository.compterMatchsAnnules(any(), any(), any())).thenReturn(0L);
        lenient().when(matchRepository.topOrganisateurs(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("genererReportingGlobal nominal → DTO complet")
    void reportingGlobalNominal() {
        stubAllSummariesToZero();
        when(transactionRepository.sommerParTypeEtPeriode(eq(TypeTransaction.RECHARGE), any(), any(), isNull()))
                .thenReturn(new BigDecimal("500.00"));
        when(matchRepository.compterMatchs(any(), any(), isNull(), isNull())).thenReturn(20L);

        ReportingDTO dto = reportingService.genererReportingGlobal(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertThat(dto.caEncaisse()).isEqualByComparingTo("500.00");
        assertThat(dto.nombreMatchsTotaux()).isEqualTo(20L);
        assertThat(dto.dateDebut()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(dto.dateFin()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("genererReportingSite nominal → filtre par siteId")
    void reportingSiteNominal() {
        stubAllSummariesToZero();
        when(transactionRepository.sommerParTypeEtPeriode(eq(TypeTransaction.RECHARGE), any(), any(), eq(1L)))
                .thenReturn(new BigDecimal("100.00"));
        when(matchRepository.compterMatchs(any(), any(), eq(1L), isNull())).thenReturn(5L);

        ReportingDTO dto = reportingService.genererReportingSite(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertThat(dto.caEncaisse()).isEqualByComparingTo("100.00");
        assertThat(dto.nombreMatchsTotaux()).isEqualTo(5L);
    }

    @Test
    @DisplayName("genererReporting compte les matchs PRIVE et PUBLIC séparément")
    void compteParTypeMatch() {
        stubAllSummariesToZero();
        when(matchRepository.compterMatchs(any(), any(), isNull(), eq(TypeMatch.PRIVE))).thenReturn(15L);
        when(matchRepository.compterMatchs(any(), any(), isNull(), eq(TypeMatch.PUBLIC))).thenReturn(8L);

        ReportingDTO dto = reportingService.genererReportingGlobal(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertThat(dto.nombreMatchsPrives()).isEqualTo(15L);
        assertThat(dto.nombreMatchsPublics()).isEqualTo(8L);
    }

    @Test
    @DisplayName("genererReporting refuse si dateDebut null")
    void refuseSiDateDebutNull() {
        assertThatThrownBy(() ->
                reportingService.genererReportingGlobal(null, LocalDate.of(2026, 5, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoires");
    }

    @Test
    @DisplayName("genererReporting refuse si dateFin < dateDebut")
    void refuseSiDateFinAvantDebut() {
        assertThatThrownBy(() ->
                reportingService.genererReportingGlobal(
                        LocalDate.of(2026, 5, 31),
                        LocalDate.of(2026, 5, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supérieure");
    }

    @Test
    @DisplayName("genererReporting limite topOrganisateurs à 5 maximum")
    void topOrganisateursLimite5() {
        stubAllSummariesToZero();
        // 7 organisateurs en DB
        when(matchRepository.topOrganisateurs(any(), any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, "S200001", "User", "1", 10L},
                        new Object[]{2L, "S200002", "User", "2", 9L},
                        new Object[]{3L, "S200003", "User", "3", 8L},
                        new Object[]{4L, "S200004", "User", "4", 7L},
                        new Object[]{5L, "S200005", "User", "5", 6L},
                        new Object[]{6L, "S200006", "User", "6", 5L},
                        new Object[]{7L, "S200007", "User", "7", 4L}));

        ReportingDTO dto = reportingService.genererReportingGlobal(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertThat(dto.topOrganisateurs()).hasSize(5);
        assertThat(dto.topOrganisateurs().get(0).matricule()).isEqualTo("S200001");
    }
}