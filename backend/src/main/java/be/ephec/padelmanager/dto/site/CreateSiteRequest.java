package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record CreateSiteRequest(
        @NotBlank(message = "Le nom du site est obligatoire.")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "L'adresse est obligatoire.")
        @Size(max = 200)
        String adresse,

        @NotBlank(message = "Le code postal est obligatoire.")
        @Size(max = 10)
        @Pattern(
                regexp = "^[0-9A-Za-z\\- ]{2,10}$",
                message = "Format de code postal invalide."
        )
        String codePostal,

        @NotBlank(message = "La ville est obligatoire.")
        @Size(max = 100)
        String ville
) {}