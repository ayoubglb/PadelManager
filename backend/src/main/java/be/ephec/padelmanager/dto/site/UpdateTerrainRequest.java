package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record UpdateTerrainRequest(
        @NotNull
        @Min(value = 1)
        Integer numero,

        @Size(max = 100)
        String nom
) {}