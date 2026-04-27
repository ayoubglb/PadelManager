package be.ephec.padelmanager.dto.site;

import java.time.LocalTime;

public record HoraireSiteDTO(
        Long id,
        Long siteId,
        Integer annee,
        LocalTime heureDebut,
        LocalTime heureFin
) {}