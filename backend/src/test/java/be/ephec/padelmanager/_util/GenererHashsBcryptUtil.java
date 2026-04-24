package be.ephec.padelmanager._util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Disabled("Utilitaire juste une fois")
class GenererHashsBcryptUtil {

    @Test
    void afficherHashs() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] motsDePasse = {"Admin2026!", "Dev2026!"};
        for (String mdp : motsDePasse) {
            System.out.println(mdp + " -> " + encoder.encode(mdp));
        }
    }
}