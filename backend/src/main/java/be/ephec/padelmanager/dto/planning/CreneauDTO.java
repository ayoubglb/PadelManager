package be.ephec.padelmanager.dto.planning;

import java.time.LocalTime;

// Bornes horaires d'un créneau de match (durée 1h30, pause 15 min)
public record CreneauDTO(LocalTime debut, LocalTime fin) {
}