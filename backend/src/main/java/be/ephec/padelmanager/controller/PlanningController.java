package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.planning.PlanningDTO;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Endpoints REST pour la consultation de la grille planning d'un site
@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planningService;

    // Retourne la grille planning du site pour la date demandée
    @GetMapping("/{siteId}/planning")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlanningDTO> consulterPlanning(
            @PathVariable Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        PlanningDTO planning = planningService.consulterPlanning(siteId, date, principal.getUtilisateur());
        return ResponseEntity.ok(planning);
    }
}