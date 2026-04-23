package be.ephec.padelmanager.dto.auth;

import be.ephec.padelmanager.entity.RoleUtilisateur;

public record AuthResponse(
        String token,
        String matricule,
        String email,
        String nom,
        String prenom,
        RoleUtilisateur role,
        long expirationMinutes
) {}