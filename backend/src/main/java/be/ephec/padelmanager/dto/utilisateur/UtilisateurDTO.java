package be.ephec.padelmanager.dto.utilisateur;

import be.ephec.padelmanager.entity.RoleUtilisateur;

import java.time.LocalDateTime;

// DTO de profil utilisateur
public record UtilisateurDTO(
        Long id,
        String matricule,
        String nom,
        String prenom,
        String email,
        String telephone,
        RoleUtilisateur role,
        Long siteRattachementId,
        String siteRattachementNom,
        Boolean active,
        LocalDateTime dateInscription
) {
}