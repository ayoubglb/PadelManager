package be.ephec.padelmanager.dto.planning;

// Cellule de la grille (croisement créneau × terrain). matchId/placesRestantes/organisateurNom sont null sauf pour PUBLIC_DISPO et COMPLET.
public record CelluleDTO(
        Long terrainId,
        StatutCellule statut,
        Long matchId,
        Integer placesRestantes,
        String organisateurNom
) {
}