package be.ephec.padelmanager.dto.planning;

import java.time.LocalTime;
import java.util.List;

// Ligne de la grille : un créneau et ses cellules (une par terrain, dans l'ordre des terrains)
public record LigneCreneauDTO(
        LocalTime debut,
        LocalTime fin,
        List<CelluleDTO> cellules
) {
}