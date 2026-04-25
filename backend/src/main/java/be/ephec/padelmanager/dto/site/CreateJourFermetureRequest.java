package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateJourFermetureRequest(
        @NotNull(message = "La date de fermeture est obligatoire.")
        LocalDate dateFermeture,

        // Null si fermeture globale, sinon id du site concerné
        Long siteId,

        @NotBlank(message = "La raison est obligatoire.")
        @Size(max = 200)
        String raison
) {}