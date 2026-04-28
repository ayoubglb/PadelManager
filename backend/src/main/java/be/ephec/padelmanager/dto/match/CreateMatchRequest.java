package be.ephec.padelmanager.dto.match;

import be.ephec.padelmanager.entity.TypeMatch;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMatchRequest(
        @NotNull(message = "Le terrain est obligatoire")
        Long terrainId,

        @NotNull(message = "La date et l'heure du match sont obligatoires")
        @Future(message = "La date du match doit être dans le futur")
        LocalDateTime dateHeureDebut,

        @NotNull(message = "Le type du match (PRIVE ou PUBLIC) est obligatoire")
        TypeMatch type
) {
}