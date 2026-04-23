package be.ephec.padelmanager.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    public static final String CLAIM_ROLE = "role";

    private final JwtProperties proprietes;

    private SecretKey cleSignature;

    private SecretKey cleSignature() {
        if (cleSignature == null) {
            byte[] bytes = proprietes.getSecret().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException(
                        "Le secret JWT doit faire au minimum 32 octets (256 bits) pour HS256 — " +
                                "longueur actuelle : " + bytes.length);
            }
            cleSignature = Keys.hmacShaKeyFor(bytes);
        }
        return cleSignature;
    }

    public String genererToken(UserDetails utilisateur) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(proprietes.getExpirationMinutes(), ChronoUnit.MINUTES);

        String role = utilisateur.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");

        return Jwts.builder()
                .subject(utilisateur.getUsername())          // matricule
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(expiration))
                .signWith(cleSignature())
                .compact();
    }

    public Optional<Claims> parser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(cleSignature())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            // Signature invalide, token expiré, malformé, etc.
            log.debug("JWT invalide : {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extraireMatricule(String token) {
        return parser(token).map(Claims::getSubject);
    }
}