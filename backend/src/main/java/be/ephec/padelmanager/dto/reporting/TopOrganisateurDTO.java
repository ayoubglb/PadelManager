package be.ephec.padelmanager.dto.reporting;

// Organisateur ayant créé le plus de matchs sur la période
public record TopOrganisateurDTO(
        Long utilisateurId,
        String matricule,
        String nomComplet,
        long nombreMatchsOrganises
) {
}