package vn.amela.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private SecretKey signingKey;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:hr-auth-service}")
    private String issuer;

    @Value("${app.jwt.audience:hr-management-system}")
    private String audience;

    /**
     * Initializes the HMAC signing key from the configured JWT secret.
     *
     * <p>Invoked after bean construction to convert the configured `secret` into the SecretKey used for verifying JWTs.</p>
     */
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts and validates the claims from a signed JWT.
     *
     * Validates the token's signature, issuer, and audience before returning the parsed payload.
     *
     * @param token the compact serialized signed JWT
     * @return the token's claims payload
     */
    public Claims extractClaims(String token) {

        return Jwts.parser()
            .verifyWith(getKey())
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Retrieves the initialized HMAC signing key.
     *
     * @return the initialized {@link javax.crypto.SecretKey} used to sign and verify JWTs
     */
    private SecretKey getKey() {

        return signingKey;
    }

}
