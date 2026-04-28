package be.ephec.padelmanager.dto.match;

import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;

import java.time.LocalDateTime;

public record MatchDTO(
        Long id,
        Long terrainId,
        Integer terrainNumero,
        Long siteId,
        String siteNom,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        Long organisateurId,
        String organisateurNom,
        TypeMatch type,
        StatutMatch statut,
        Boolean devenuPublicAutomatiquement,
        LocalDateTime dateCreation,
        boolean termine
) {
}