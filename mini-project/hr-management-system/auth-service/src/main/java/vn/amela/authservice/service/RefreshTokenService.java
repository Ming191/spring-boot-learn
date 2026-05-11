package vn.amela.authservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.amela.authservice.entity.RefreshToken;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.mapper.RefreshTokenMapper;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenMapper refreshTokenMapper;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    public String createRefreshToken(User user) {

        String rawToken = generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUserId(user.getId());

        refreshToken.setTokenHash(hashToken(rawToken));

        refreshToken.setExpiresAt(
            LocalDateTime.now().plusDays(refreshExpirationDays)
        );

        refreshTokenMapper.insert(refreshToken);

        return rawToken;
    }

    public String hashToken(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
