package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.site.CreateJourFermetureRequest;
import be.ephec.padelmanager.dto.site.JourFermetureDTO;
import be.ephec.padelmanager.service.JourFermetureService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Jours de fermeture", description = "Fermetures globales et site-spécifiques")
public class JourFermetureController {

    private final JourFermetureService jourFermetureService;

    @GetMapping("/jours-fermeture/globaux")
    @Operation(summary = "Liste des fermetures globales (tous sites confondus)")
    public List<JourFermetureDTO> listerGlobales() {
        return jourFermetureService.listerToutesGlobales();
    }

    @GetMapping("/sites/{siteId}/jours-fermeture")
    @Operation(summary = "Fermetures spécifiques à un site (hors globales)")
    public List<JourFermetureDTO> listerParSite(@PathVariable Long siteId) {
        return jourFermetureService.listerParSite(siteId);
    }

    @PostMapping("/jours-fermeture")
    @PreAuthorize(
            "(#requete.siteId() == null and hasRole('ADMIN_GLOBAL')) "
                    + "or (#requete.siteId() != null and hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE'))"
    )
    @Operation(summary = "Création d'une fermeture (globale ou site-spécifique)")
    public ResponseEntity<JourFermetureDTO> creer(@Valid @RequestBody CreateJourFermetureRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jourFermetureService.creer(requete));
    }

    @DeleteMapping("/jours-fermeture/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Suppression d'une fermeture")
    public void supprimer(@PathVariable Long id) {
        jourFermetureService.supprimer(id);
    }
}