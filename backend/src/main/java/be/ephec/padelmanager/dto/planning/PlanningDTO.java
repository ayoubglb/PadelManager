package be.ephec.padelmanager.dto.planning;

import java.time.LocalDate;
import java.util.List;

// Planning d'un site à une date. Si ferme=true, raison est rempli et creneaux est vide
public record PlanningDTO(
        Long siteId,
        String siteNom,
        LocalDate date,
        boolean ferme,
        String raison,
        List<TerrainPlanningDTO> terrains,
        List<LigneCreneauDTO> creneaux
) {
}