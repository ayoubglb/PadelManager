package be.ephec.padelmanager.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ReponseErreur(
        String code,
        String message,
        List<String> details,
        OffsetDateTime timestamp
) {

    // Constructeur pour les erreurs simples
    public static ReponseErreur de(String code, String message) {
        return new ReponseErreur(code, message, List.of(), OffsetDateTime.now());
    }

    // Constructeur pour les erreurs de validation avec liste de champs invalides
    public static ReponseErreur deValidation(String message, List<String> details) {
        return new ReponseErreur("VALIDATION_ERROR", message, details, OffsetDateTime.now());
    }
}