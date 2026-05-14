package be.ephec.padelmanager.dto.utilisateur;

import be.ephec.padelmanager.entity.RoleUtilisateur;

// DTO restreint pour la recherche d'utilisateurs invitables à un match. Ne contient PAS l'email ni le téléphone
public record UtilisateurRechercheDTO(
        Long id,
        String matricule,
        String nom,
        String prenom,
        RoleUtilisateur role
) {
}