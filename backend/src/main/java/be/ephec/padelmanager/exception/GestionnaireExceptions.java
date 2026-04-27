package be.ephec.padelmanager.exception;

import be.ephec.padelmanager.dto.ReponseErreur;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

// Gestionnaire global des exceptions REST de l'application.

 // IllegalArgumentException} → 400 Bad Request (règle métier violée)
 // MethodArgumentNotValidException} → 400 avec détails des champs (validation Bean Validation)
 // BadCredentialsException} → 401 Unauthorized (login/mot de passe invalide)
 // AccessDeniedException} → 403 Forbidden (autorisation refusée)
 // EntityNotFoundException} → 404 Not Found
 // Exception} (catch-all) → 500 Internal Server Error
@RestControllerAdvice
@Slf4j
public class GestionnaireExceptions {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ReponseErreur> regleMetierViolee(IllegalArgumentException ex) {
        log.debug("Règle métier violée : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ReponseErreur.de("BUSINESS_RULE_VIOLATED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReponseErreur> erreursValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.debug("Erreurs de validation : {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ReponseErreur.deValidation("Données invalides.", details));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ReponseErreur> credentialsInvalides(BadCredentialsException ex) {
        log.debug("Tentative de connexion avec credentials invalides");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ReponseErreur.de("INVALID_CREDENTIALS", "Identifiants invalides."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ReponseErreur> accesRefuse(AccessDeniedException ex) {
        log.debug("Accès refusé : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ReponseErreur.de("ACCESS_DENIED",
                        "Vous n'avez pas les droits nécessaires pour effectuer cette action."));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ReponseErreur> entiteIntrouvable(EntityNotFoundException ex) {
        log.debug("Entité introuvable : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ReponseErreur.de("NOT_FOUND", ex.getMessage()));
    }

    // Sécurité pour les exceptions imprévues
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> erreurInattendue(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReponseErreur.de("INTERNAL_ERROR",
                        "Une erreur interne est survenue. Veuillez réessayer plus tard."));
    }
}