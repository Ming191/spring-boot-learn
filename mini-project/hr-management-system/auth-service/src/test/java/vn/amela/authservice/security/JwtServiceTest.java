package vn.amela.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.entity.enums.Role;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";

    @Test
    @DisplayName("generates access token with required auth claims")
    void generatesAccessToken() {
        JwtService jwtService = jwtService(900_000L);
        User user = user();

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractId(token)).isEqualTo(1L);
        assertThat(jwtService.extractUsername(token)).isEqualTo("emp");
        assertThat(jwtService.extractRole(token)).isEqualTo("EMPLOYEE");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(900L);
    }

    @Test
    @DisplayName("marks expired token as invalid")
    void rejectsExpiredToken() {
        JwtService jwtService = jwtService(-1_000L);

        String token = jwtService.generateAccessToken(user());

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("marks token signed by another secret as invalid")
    void rejectsWrongSignature() {
        JwtService issuer = jwtService(900_000L);
        JwtService verifier = jwtService(900_000L);
        ReflectionTestUtils.setField(verifier, "secret", "another-secret-key-must-be-at-least-32-chars");
        verifier.init();

        String token = issuer.generateAccessToken(user());

        assertThat(verifier.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("marks token from another issuer as invalid")
    void rejectsWrongIssuer() {
        JwtService jwtService = jwtService(900_000L);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900_000L);
        String token = Jwts.builder()
            .subject("1")
            .claim("username", "emp")
            .claim("role", "EMPLOYEE")
            .issuer("another-service")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    private static JwtService jwtService(long expirationMs) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", expirationMs);
        jwtService.init();
        return jwtService;
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("emp");
        user.setRole(Role.EMPLOYEE);
        return user;
    }
}
