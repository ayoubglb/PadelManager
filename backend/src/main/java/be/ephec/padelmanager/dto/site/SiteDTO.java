package be.ephec.padelmanager.dto.site;

// DTO lecture site
public record SiteDTO(
        Long id,
        String nom,
        String adresse,
        String codePostal,
        String ville,
        Boolean active
) {}