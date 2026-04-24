package be.ephec.padelmanager.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties proprietes;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requete,
                                    @NonNull HttpServletResponse reponse,
                                    @NonNull FilterChain chaine) throws ServletException, IOException {

        extraireToken(requete).ifPresent(token ->
                jwtService.extraireMatricule(token).ifPresent(matricule -> {
                    // Ne ré-authentifie pas si un SecurityContext a déjà été peuplé en amont
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        authentifier(matricule);
                    }
                })
        );

        chaine.doFilter(requete, reponse);
    }

    private java.util.Optional<String> extraireToken(HttpServletRequest requete) {
        String header = requete.getHeader(proprietes.getHeader());
        if (header == null || !header.startsWith(proprietes.getPrefixe())) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(header.substring(proprietes.getPrefixe().length()));
    }

    private void authentifier(String matricule) {
        try {
            UserDetails utilisateur = userDetailsService.loadUserByUsername(matricule);

            // L'utilisateur peut avoir été anonymisé (RGPD) après émission du token
            if (!utilisateur.isEnabled()) {
                return;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    utilisateur, null, utilisateur.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignore) {
            // Utilisateur supprimé entre l'émission du token et sa présentation
        }
    }


}