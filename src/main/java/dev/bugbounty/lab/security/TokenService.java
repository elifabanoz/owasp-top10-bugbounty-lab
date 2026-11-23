package dev.bugbounty.lab.security;

import dev.bugbounty.lab.config.LabMode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final String LAB_SECRET = "local-lab-predictable-secret-0000";
    private final LabMode mode;
    private final SecretKey key;

    public TokenService(LabMode mode, @Value("${lab.jwt-secret:}") String configuredSecret) {
        this.mode = mode;
        String secret = mode.vulnerable() ? LAB_SECRET : configuredSecret;
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Secure mode requires LAB_JWT_SECRET of at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(long userId) {
        var builder = Jwts.builder().subject(Long.toString(userId)).issuedAt(Date.from(Instant.now()));
        if (!mode.vulnerable()) {
            builder.expiration(Date.from(Instant.now().plusSeconds(900)));
        }
        // intentionally vulnerable: predictable signing key and no expiry in the local lab profile
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    public long verify(String token) {
        var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        if (!mode.vulnerable() && !"HS256".equals(parsed.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("Unexpected token algorithm");
        }
        Claims claims = parsed.getPayload();
        if (!mode.vulnerable() && claims.getExpiration() == null) {
            throw new IllegalArgumentException("Token has no expiry");
        }
        return Long.parseLong(claims.getSubject());
    }
}
