package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateTerrainRequest(
        @NotNull(message = "Le numéro du terrain est obligatoire.")
        @Min(value = 1, message = "Le numéro doit être supérieur à 0.")
        Integer numero,

        // Optionel dans la DB
        @Size(max = 100)
        String nom
) {}