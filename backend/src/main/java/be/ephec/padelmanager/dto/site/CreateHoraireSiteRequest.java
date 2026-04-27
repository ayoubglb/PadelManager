package be.ephec.padelmanager.dto.site;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;


public record CreateHoraireSiteRequest(
        @NotNull
        @Min(2000)
        @Max(2100)
        Integer annee,

        @NotNull
        LocalTime heureDebut,

        @NotNull
        LocalTime heureFin
) {}