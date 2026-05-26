package vn.amela.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private static final String JWT_ALGORITHM = "HS256";

    private SecretKey signingKey;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:hr-auth-service}")
    private String issuer;

    @Value("${app.jwt.audience:hr-management-system}")
    private String audience;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {

        Jws<Claims> claimsJws = Jwts.parser()
            .verifyWith(getKey())
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseSignedClaims(token);

        if (!JWT_ALGORITHM.equals(claimsJws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Unsupported JWT algorithm");
        }

        return claimsJws.getPayload();
    }

    private SecretKey getKey() {

        return signingKey;
    }

}
