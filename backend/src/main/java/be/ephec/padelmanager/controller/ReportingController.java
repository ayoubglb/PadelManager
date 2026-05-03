package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.reporting.ReportingDTO;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    // Reporting global tous sites confondus. Réservé à ADMIN_GLOBAL
    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<ReportingDTO> reportingGlobal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        return ResponseEntity.ok(reportingService.genererReportingGlobal(dateDebut, dateFin));
    }

    // Reporting d'un site spécifique
    //  ADMIN_GLOBAL : peut consulter n'importe quel site.
    //  ADMIN_SITE : peut consulter uniquement son site rattaché
    @GetMapping("/sites/{siteId}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    public ResponseEntity<ReportingDTO> reportingSite(
            @PathVariable Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        // ADMIN_SITE : limité à son site rattaché
        if (principal.getUtilisateur().getRole() == RoleUtilisateur.ADMIN_SITE) {
            Long monSiteId = principal.getUtilisateur().getSiteRattachement() != null
                    ? principal.getUtilisateur().getSiteRattachement().getId() : null;
            if (monSiteId == null || !monSiteId.equals(siteId)) {
                throw new AccessDeniedException(
                        "Vous ne pouvez consulter que le reporting de votre site rattaché");
            }
        }

        return ResponseEntity.ok(reportingService.genererReportingSite(siteId, dateDebut, dateFin));
    }
}