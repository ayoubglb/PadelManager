package be.ephec.padelmanager.dto.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// DTO de reporting financier et statistique pour l'admin
public record ReportingDTO(
        // Période du reporting
        LocalDate dateDebut,
        LocalDate dateFin,

        // Métriques financières
        BigDecimal caEncaisse,            // SUM(RECHARGE) — argent réel reçu
        BigDecimal volumeMatchs,          // SUM(PAIEMENT_MATCH + SOLDE_DU_ORGANISATEUR)

        // Statistiques matchs
        long nombreMatchsTotaux,
        long nombreMatchsPrives,
        long nombreMatchsPublics,
        long nombreMatchsAnnules,

        // Top 5 organisateurs
        List<TopOrganisateurDTO> topOrganisateurs
) {
}