package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.reporting.ReportingDTO;
import be.ephec.padelmanager.dto.reporting.TopOrganisateurDTO;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Service de reporting financier et statistique pour les admins
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private static final int LIMITE_TOP = 5;

    private final TransactionRepository transactionRepository;
    private final MatchRepository matchRepository;

    // Reporting global : tous les sites confondus
    @Transactional(readOnly = true)
    public ReportingDTO genererReportingGlobal(LocalDate dateDebut, LocalDate dateFin) {
        return genererReporting(dateDebut, dateFin, null);
    }

    // Reporting pour un site spécifique
    @Transactional(readOnly = true)
    public ReportingDTO genererReportingSite(Long siteId, LocalDate dateDebut, LocalDate dateFin) {
        return genererReporting(dateDebut, dateFin, siteId);
    }

    // Méthode interne mutualisée
    private ReportingDTO genererReporting(LocalDate dateDebut, LocalDate dateFin, Long siteId) {
        validerPeriode(dateDebut, dateFin);

        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        // Métriques financières
        BigDecimal caEncaisse = transactionRepository.sommerParTypeEtPeriode(
                TypeTransaction.RECHARGE, debut, fin, siteId);
        BigDecimal volumeMatchs = transactionRepository.sommerParTypesEtPeriode(
                List.of(TypeTransaction.PAIEMENT_MATCH, TypeTransaction.SOLDE_DU_ORGANISATEUR),
                debut, fin, siteId);

        // Stats matchs
        long nombreMatchsTotaux = matchRepository.compterMatchs(debut, fin, siteId, null);
        long nombreMatchsPrives = matchRepository.compterMatchs(debut, fin, siteId, TypeMatch.PRIVE);
        long nombreMatchsPublics = matchRepository.compterMatchs(debut, fin, siteId, TypeMatch.PUBLIC);
        long nombreMatchsAnnules = matchRepository.compterMatchsAnnules(debut, fin, siteId);

        // Top 5 organisateurs
        List<TopOrganisateurDTO> topOrganisateurs = matchRepository
                .topOrganisateurs(debut, fin, siteId).stream()
                .limit(LIMITE_TOP)
                .map(row -> new TopOrganisateurDTO(
                        (Long) row[0],
                        (String) row[1],
                        row[2] + " " + row[3],
                        (Long) row[4]))
                .toList();

        log.info("Reporting généré : siteId={}, période={} → {}, CA={}€, matchs={}",
                siteId, dateDebut, dateFin, caEncaisse, nombreMatchsTotaux);

        return new ReportingDTO(
                dateDebut, dateFin,
                caEncaisse, volumeMatchs,
                nombreMatchsTotaux, nombreMatchsPrives, nombreMatchsPublics, nombreMatchsAnnules,
                topOrganisateurs);
    }

    private void validerPeriode(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("dateDebut et dateFin sont obligatoires");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "dateFin doit être supérieure ou égale à dateDebut");
        }
    }
}