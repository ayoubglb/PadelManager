package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.site.CreateTerrainRequest;
import be.ephec.padelmanager.dto.site.TerrainDTO;
import be.ephec.padelmanager.dto.site.UpdateTerrainRequest;
import be.ephec.padelmanager.service.TerrainService;
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
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Terrains", description = "Gestion des terrains d'un site")
public class TerrainController {

    private final TerrainService terrainService;

    // -------- Lecture sous /sites/{siteId}/terrains --------

    @GetMapping("/sites/{siteId}/terrains")
    @Operation(summary = "Terrains actifs d'un site (public)")
    public List<TerrainDTO> listerActifsParSite(@PathVariable Long siteId) {
        return terrainService.listerActifsParSite(siteId);
    }

    @GetMapping("/sites/{siteId}/terrains/admin")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Tous les terrains d'un site, actifs et désactivés (admin)")
    public List<TerrainDTO> listerTousParSite(@PathVariable Long siteId) {
        return terrainService.listerTousParSite(siteId);
    }

    @PostMapping("/sites/{siteId}/terrains")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Création d'un terrain dans un site (CF-RS-005)")
    public ResponseEntity<TerrainDTO> creer(
            @PathVariable Long siteId,
            @Valid @RequestBody CreateTerrainRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(terrainService.creer(siteId, requete, principal.getUtilisateur()));
    }

    // -------- Opérations individuelles sous /terrains/{id} --------

    @GetMapping("/terrains/{id}")
    @Operation(summary = "Détails d'un terrain (public)")
    public TerrainDTO recuperer(@PathVariable Long id) {
        return terrainService.recuperer(id);
    }

    @PutMapping("/terrains/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Mise à jour d'un terrain")
    public TerrainDTO mettreAJour(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTerrainRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return terrainService.mettreAJour(id, requete, principal.getUtilisateur());
    }

    @PutMapping("/terrains/{id}/activer")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Réactivation d'un terrain désactivé")
    public TerrainDTO activer(
            @PathVariable Long id,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return terrainService.activer(id, principal.getUtilisateur());
    }

    @DeleteMapping("/terrains/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Désactivation d'un terrain (maintenance)")
    public TerrainDTO desactiver(
            @PathVariable Long id,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return terrainService.desactiver(id, principal.getUtilisateur());
    }
}