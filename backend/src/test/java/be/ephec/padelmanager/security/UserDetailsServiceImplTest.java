package be.ephec.padelmanager.security;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl — résolution email/matricule + RGPD")
class UserDetailsServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Utilisateur actif;

    @BeforeEach
    void setUp() {
        actif = Utilisateur.builder()
                .id(1L)
                .matricule("G000001")
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@example.com")
                .telephone("+32475123456")
                .passwordHash("$2a$10$fakeBcryptHashForTestingOnly")
                .role(RoleUtilisateur.MEMBRE_GLOBAL)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Login par email → UserDetails valide avec ROLE_MEMBRE_GLOBAL")
    void chargeUtilisateurParEmail() {
        when(utilisateurRepository.findByEmailOrMatricule("jean.dupont@example.com"))
                .thenReturn(Optional.of(actif));

        UserDetails details = userDetailsService.loadUserByUsername("jean.dupont@example.com");

        assertThat(details.getUsername()).isEqualTo("G000001"); // matricule, pas email
        assertThat(details.getPassword()).isEqualTo("$2a$10$fakeBcryptHashForTestingOnly");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_MEMBRE_GLOBAL");
    }

    @Test
    @DisplayName("Login par matricule → même résultat que par email")
    void chargeUtilisateurParMatricule() {
        when(utilisateurRepository.findByEmailOrMatricule("G000001"))
                .thenReturn(Optional.of(actif));

        UserDetails details = userDetailsService.loadUserByUsername("G000001");

        assertThat(details.getUsername()).isEqualTo("G000001");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Utilisateur inconnu → UsernameNotFoundException")
    void leveUsernameNotFoundSiAucunUtilisateur() {
        when(utilisateurRepository.findByEmailOrMatricule(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inconnu@nowhere.be"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Compte anonymisé RGPD (active=false) → isEnabled() false")
    void utilisateurAnonymiseEstDisabled() {
        actif.setActive(false);
        when(utilisateurRepository.findByEmailOrMatricule("jean.dupont@example.com"))
                .thenReturn(Optional.of(actif));

        UserDetails details = userDetailsService.loadUserByUsername("jean.dupont@example.com");

        assertThat(details.isEnabled()).isFalse();
    }
}