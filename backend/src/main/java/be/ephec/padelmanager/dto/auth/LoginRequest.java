package be.ephec.padelmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'identifiant (email ou matricule) est obligatoire.")
        String login,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String motDePasse
) {}