package be.ephec.padelmanager.service.auth;

import be.ephec.padelmanager.dto.auth.AuthResponse;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.AuthMapper;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.security.jwt.JwtProperties;
import be.ephec.padelmanager.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — inscription, connexion, règles métier")
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthMapper authMapper;

    @InjectMocks
    private AuthService authService;

    // -------- Inscription : cas nominal --------

    @Test
    @DisplayName("Inscription MEMBRE_LIBRE → matricule généré avec préfixe L + token émis")
    void inscriptionMembreLibreReussit() {
        RegisterRequest req = new RegisterRequest(
                "Dupont", "Jean", "jean@example.com", "+32475123456",
                "motdepasse123", RoleUtilisateur.MEMBRE_LIBRE, null
        );
        when(utilisateurRepository.existsByEmail("jean@example.com")).thenReturn(false);
        when(utilisateurRepository.existsByMatricule(anyString())).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("$2a$10$hash");
        when(utilisateurRepository.save(any(Utilisateur.class)))
                .thenAnswer(inv -> {
                    Utilisateur u = inv.getArgument(0);
                    u.setId(1L);
                    return u;
                });
        when(jwtService.genererToken(any())).thenReturn("fake.jwt.token");
        when(jwtProperties.getExpirationMinutes()).thenReturn(60L);

        AuthResponse reponseAttendue = new AuthResponse(
                "fake.jwt.token", "L000000", "jean@example.com",
                "Dupont", "Jean", RoleUtilisateur.MEMBRE_LIBRE, 60L
        );
        when(authMapper.versReponse(any(Utilisateur.class), eq("fake.jwt.token"), eq(60L)))
                .thenReturn(reponseAttendue);

        AuthResponse reponse = authService.inscrire(req);

        // Vérifications sur l'entité effectivement sauvegardée (logique métier d'AuthService)
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur sauve = captor.getValue();
        assertThat(sauve.getMatricule()).matches("^L\\d{6}$");
        assertThat(sauve.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(sauve.getActive()).isTrue();

        // Vérification que la réponse vient bien du mapper
        assertThat(reponse).isSameAs(reponseAttendue);
    }

    // -------- Inscription : règles de sécurité --------

    @Test
    @DisplayName("Tentative d'inscription ADMIN_GLOBAL → refusée (CF-AA-006)")
    void inscriptionAdminRefusee() {
        RegisterRequest req = new RegisterRequest(
                "Admin", "Root", "admin@example.com", "+32475000000",
                "motdepasse123", RoleUtilisateur.ADMIN_GLOBAL, null
        );

        assertThatThrownBy(() -> authService.inscrire(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membre");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("MEMBRE_SITE sans siteRattachementId → refusé (CF-RS-017)")
    void membreSiteExigeSiteRattachement() {
        RegisterRequest req = new RegisterRequest(
                "Dupont", "Jean", "jean@example.com", "+32475123456",
                "motdepasse123", RoleUtilisateur.MEMBRE_SITE, null
        );

        assertThatThrownBy(() -> authService.inscrire(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("siteRattachementId");
    }

    @Test
    @DisplayName("MEMBRE_GLOBAL avec siteRattachementId → refusé (CF-RS-018)")
    void membreGlobalInterditSiteRattachement() {
        RegisterRequest req = new RegisterRequest(
                "Dupont", "Jean", "jean@example.com", "+32475123456",
                "motdepasse123", RoleUtilisateur.MEMBRE_GLOBAL, 42L
        );

        assertThatThrownBy(() -> authService.inscrire(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ne doit pas");
    }

    @Test
    @DisplayName("Email déjà utilisé → refusé")
    void emailDejaUtiliseRefuse() {
        RegisterRequest req = new RegisterRequest(
                "Dupont", "Jean", "jean@example.com", "+32475123456",
                "motdepasse123", RoleUtilisateur.MEMBRE_LIBRE, null
        );
        when(utilisateurRepository.existsByEmail("jean@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.inscrire(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(utilisateurRepository, never()).save(any());
    }

    // -------- Connexion --------

    @Test
    @DisplayName("Connexion valide → token émis avec matricule et rôle")
    void connexionReussie() {
        LoginRequest req = new LoginRequest("jean@example.com", "motdepasse123");

        Utilisateur u = Utilisateur.builder()
                .id(1L).matricule("G000001").email("jean@example.com")
                .nom("Dupont").prenom("Jean").telephone("+32475123456")
                .passwordHash("$2a$10$hash").role(RoleUtilisateur.MEMBRE_GLOBAL)
                .active(true)
                .build();
        UtilisateurPrincipal principal = new UtilisateurPrincipal(u);

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(jwtService.genererToken(principal)).thenReturn("fake.jwt.token");
        when(jwtProperties.getExpirationMinutes()).thenReturn(60L);

        AuthResponse reponseAttendue = new AuthResponse(
                "fake.jwt.token", "G000001", "jean@example.com",
                "Dupont", "Jean", RoleUtilisateur.MEMBRE_GLOBAL, 60L
        );
        when(authMapper.versReponse(u, "fake.jwt.token", 60L))
                .thenReturn(reponseAttendue);

        AuthResponse reponse = authService.connecter(req);

        assertThat(reponse).isSameAs(reponseAttendue);
    }
}