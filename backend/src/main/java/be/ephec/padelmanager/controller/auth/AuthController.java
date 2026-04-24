package be.ephec.padelmanager.controller.auth;

import be.ephec.padelmanager.dto.auth.AuthResponse;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion des utilisateurs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Inscription publique d'un membre (Libre/Site/Global)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest requete) {
        AuthResponse reponse = authService.inscrire(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion par email ou matricule")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest requete) {
        return ResponseEntity.ok(authService.connecter(requete));
    }
}