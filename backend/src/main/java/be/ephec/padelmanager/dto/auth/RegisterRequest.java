package be.ephec.padelmanager.dto.auth;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100)
        String prenom,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Format d'email invalide.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Le téléphone est obligatoire.")
        @Size(max = 30)
        @Pattern(
                regexp = "^\\+?[0-9 .\\-()]{6,30}$",
                message = "Format de téléphone invalide."
        )
        String telephone,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères.")
        String motDePasse,

        @NotNull(message = "Le rôle est obligatoire.")
        RoleUtilisateur role,

        // Obligatoire si role = MEMBRE_SITE, sinon doit être null.
        Long siteRattachementId
) {}