package be.ephec.padelmanager.dto.penalite;

import java.time.LocalDateTime;

// DTO de pénalité utilisateur
public record PenaliteDTO(
        Long id,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        String motif,
        Long matchId,                 // Match d'origine (nullable si pénalité administrative)
        Boolean active,               // true si maintenant ∈ [dateDebut, dateFin]
        LocalDateTime dateCreation
) {
}