package vn.amela.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.amela.authservice.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "hr-auth-service";
    private static final String AUDIENCE = "hr-management-system";
    private SecretKey signingKey;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .issuer(ISSUER)
            .issuedAt(now)
            .audience().add(AUDIENCE).and()
            .expiration(expiry)
            .id(UUID.randomUUID().toString())
            .signWith(getKey())
            .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .requireIssuer(ISSUER)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long extractId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private SecretKey getKey() {
        return signingKey;
    }
}
