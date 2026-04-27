package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.site.CreateHoraireSiteRequest;
import be.ephec.padelmanager.dto.site.HoraireSiteDTO;
import be.ephec.padelmanager.service.HoraireSiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Horaires", description = "Horaires d'ouverture des sites par année")
public class HoraireSiteController {

    private final HoraireSiteService horaireSiteService;

    @GetMapping("/sites/{siteId}/horaires")
    @Operation(summary = "Liste des horaires d'un site, toutes années (public)")
    public List<HoraireSiteDTO> listerParSite(@PathVariable Long siteId) {
        return horaireSiteService.listerParSite(siteId);
    }

    @GetMapping("/sites/{siteId}/horaires/{annee}")
    @Operation(summary = "Horaire d'un site pour une année donnée (utilisé par la grille planning)")
    public HoraireSiteDTO recupererParAnnee(@PathVariable Long siteId,
                                            @PathVariable Integer annee) {
        return horaireSiteService.recupererParSiteEtAnnee(siteId, annee);
    }

    @PostMapping("/sites/{siteId}/horaires")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @Operation(summary = "Création d'un horaire pour une année (CF-RS-008)")
    public ResponseEntity<HoraireSiteDTO> creer(@PathVariable Long siteId,
                                                @Valid @RequestBody CreateHoraireSiteRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horaireSiteService.creer(siteId, requete));
    }

    @DeleteMapping("/horaires/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Suppression d'un horaire")
    public void supprimer(@PathVariable Long id) {
        horaireSiteService.supprimer(id);
    }
}