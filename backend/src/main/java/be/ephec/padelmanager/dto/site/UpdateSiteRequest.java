package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record UpdateSiteRequest(
        @NotBlank @Size(max = 100)  String nom,
        @NotBlank @Size(max = 200)  String adresse,
        @NotBlank @Size(max = 10)
        @Pattern(regexp = "^[0-9A-Za-z\\- ]{2,10}$")
        String codePostal,
        @NotBlank @Size(max = 100)  String ville
) {}