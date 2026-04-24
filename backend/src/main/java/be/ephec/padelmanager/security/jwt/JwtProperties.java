package be.ephec.padelmanager.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
@ConfigurationProperties(prefix = "padelmanager.securite.jwt")
public class JwtProperties {

    private String secret;

    private long expirationMinutes = 60;

    private String header = "Authorization";

    private String prefixe = "Bearer ";
}