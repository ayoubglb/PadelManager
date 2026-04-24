package be.ephec.padelmanager.security.jwt;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService — génération, parsing, signature")
class JwtServiceTest {

    private JwtService jwtService;
    private UtilisateurPrincipal principal;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-at-least-32-chars-long-for-hs256!");
        props.setExpirationMinutes(60);

        jwtService = new JwtService(props);

        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L)
                .matricule("G000001")
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@example.com")
                .telephone("+32475123456")
                .passwordHash("fake-hash")
                .role(RoleUtilisateur.MEMBRE_GLOBAL)
                .active(true)
                .build();
        principal = new UtilisateurPrincipal(utilisateur);
    }

    @Test
    @DisplayName("Génération puis parsing → matricule et rôle préservés")
    void roundTrip() {
        String token = jwtService.genererToken(principal);

        Optional<Claims> claims = jwtService.parser(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("G000001");
        assertThat(claims.get().get(JwtService.CLAIM_ROLE, String.class))
                .isEqualTo("ROLE_MEMBRE_GLOBAL");
    }

    @Test
    @DisplayName("extraireMatricule() renvoie le sub du token")
    void extraireMatricule() {
        String token = jwtService.genererToken(principal);

        assertThat(jwtService.extraireMatricule(token)).contains("G000001");
    }

    @Test
    @DisplayName("Token falsifié (signature modifiée) → parser() vide, pas d'exception")
    void tokenFalsifieRejete() {
        String token = jwtService.genererToken(principal);

        // Altère un caractère au milieu de la signature (partie après le dernier '.')
        int debutSignature = token.lastIndexOf('.') + 1;
        int milieu = debutSignature + (token.length() - debutSignature) / 2;
        char original = token.charAt(milieu);
        char remplacement = (original == 'A' ? 'B' : 'A');
        String falsifie = token.substring(0, milieu) + remplacement + token.substring(milieu + 1);

        assertThat(jwtService.parser(falsifie)).isEmpty();
        assertThat(jwtService.extraireMatricule(falsifie)).isEmpty();
    }

    @Test
    @DisplayName("Token malformé (non-JWT) → parser() vide")
    void tokenMalformeRejete() {
        assertThat(jwtService.parser("ceci-n-est-pas-un-jwt")).isEmpty();
    }

    @Test
    @DisplayName("Secret trop court → IllegalStateException à la première utilisation")
    void secretTropCourtLeveException() {
        JwtProperties propsInvalides = new JwtProperties();
        propsInvalides.setSecret("trop-court");
        JwtService serviceInvalide = new JwtService(propsInvalides);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> serviceInvalide.genererToken(principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 octets");
    }
}