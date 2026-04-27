package be.ephec.padelmanager.dto.site;

import java.time.LocalDate;

public record JourFermetureDTO(
        Long id,
        LocalDate dateFermeture,
        Long siteId,
        String raison
) {}