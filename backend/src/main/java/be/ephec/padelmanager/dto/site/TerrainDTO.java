package be.ephec.padelmanager.dto.site;


public record TerrainDTO(
        Long id,
        Integer numero,
        String nom,
        Long siteId,
        Boolean active
) {}