package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.planning.TerrainPlanningDTO;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.Terrain;
import org.mapstruct.Mapper;

// Mapper MapStruct pour les conversions entité → DTO de la grille planning
@Mapper(componentModel = "spring")
public interface PlanningMapper {

    // Convertit un Terrain en TerrainPlanningDTO (id, numéro, nom)
    TerrainPlanningDTO toTerrainDto(Terrain terrain);

    // Concatène prénom et nom de l'organisateur d'un match pour affichage
    default String nomOrganisateur(Match match) {
        return match.getOrganisateur().getPrenom() + " " + match.getOrganisateur().getNom();
    }
}