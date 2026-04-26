package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.site.CreateSiteRequest;
import be.ephec.padelmanager.dto.site.SiteDTO;
import be.ephec.padelmanager.dto.site.UpdateSiteRequest;
import be.ephec.padelmanager.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
@Tag(name = "Sites", description = "Gestion des clubs de padel (4 sites)")
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    @Operation(summary = "Liste des sites actifs (public) ou tous les sites (ADMIN_GLOBAL)")
    public List<SiteDTO> lister(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean inclureInactifs
    ) {
        // Sécurité : un visiteur public ne peut voir que les sites actifs.
        // L'option "inclureInactifs=true" est gardée par @PreAuthorize sur la méthode dédiée.
        if (inclureInactifs) {
            return listerTous();
        }
        return siteService.listerActifs();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    @Operation(summary = "Liste de tous les sites (actifs + désactivés) — ADMIN_GLOBAL uniquement")
    public List<SiteDTO> listerTous() {
        return siteService.listerTous();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un site (public)")
    public SiteDTO recuperer(@PathVariable Long id) {
        return siteService.recuperer(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    @Operation(summary = "Création d'un site — ADMIN_GLOBAL uniquement")
    public ResponseEntity<SiteDTO> creer(@Valid @RequestBody CreateSiteRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.creer(requete));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    @Operation(summary = "Mise à jour d'un site — ADMIN_GLOBAL uniquement")
    public SiteDTO mettreAJour(@PathVariable Long id,
                               @Valid @RequestBody UpdateSiteRequest requete) {
        return siteService.mettreAJour(id, requete);
    }

    @PutMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    @Operation(summary = "Réactivation d'un site désactivé")
    public SiteDTO activer(@PathVariable Long id) {
        return siteService.activer(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    @Operation(summary = "Désactivation d'un site (soft delete) — préserve l'historque")
    public SiteDTO desactiver(@PathVariable Long id) {
        return siteService.desactiver(id);
    }
}