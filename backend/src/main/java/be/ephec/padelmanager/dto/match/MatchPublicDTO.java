package be.ephec.padelmanager.dto.match;

import java.time.LocalDateTime;

// DTO enrichi pour le catalogue des matchs publics
public record MatchPublicDTO(
        Long id,
        Long siteId,
        String siteNom,
        Integer terrainNumero,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        String organisateurNom,
        Integer placesRestantes
) {
}