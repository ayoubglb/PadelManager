package be.ephec.padelmanager.dto.inscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Payload pour inviter un joueur à un match privé
public record InviterJoueurRequest(

        @NotBlank(message = "Le matricule du joueur est obligatoire")
        @Pattern(regexp = "^(G|S|L|AS|AG)\\d{6}$",
                message = "Le matricule doit suivre le format préfixe + 6 chiffres")
        String matricule
) {
}