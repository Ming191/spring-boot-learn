package vn.amela.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.authservice.dto.request.LoginRequest;
import vn.amela.authservice.dto.request.RefreshRequest;
import vn.amela.authservice.dto.request.RegisterRequest;
import vn.amela.authservice.dto.response.TokenResponse;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.entity.RefreshToken;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.mapper.RefreshTokenMapper;
import vn.amela.authservice.mapper.UserMapper;
import vn.amela.authservice.security.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_PREFIX = "Bearer";
    private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenMapper refreshTokenMapper;

    public UserResponse register(RegisterRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        Role role = Role.EMPLOYEE;

        if (userMapper.selectByUserName(username) != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Username already exists"
            );
        }

        if (userMapper.selectByEmail(email) != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already exists"
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setIsActive(true);

        userMapper.insert(user);

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .build();
    }

    public TokenResponse login(LoginRequest request) {

        User user = userMapper.selectByUserNameOrEmail(request.getUsernameOrEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
        }

        refreshTokenMapper.revokeAllByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenResponse(
            accessToken,
            refreshToken,
            TOKEN_PREFIX,
            jwtService.getExpirationSeconds()
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String rawToken = extractRefreshToken(request);
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken storedToken = refreshTokenMapper.selectByTokenHash(tokenHash);
        if (storedToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
        }

        if (storedToken.getRevokedAt() != null) {
            refreshTokenMapper.revokeAllByUserId(storedToken.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (storedToken.getExpiresAt() == null || storedToken.getExpiresAt().isBefore(now)) {
            refreshTokenMapper.revokeById(storedToken.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
        }

        User user = userMapper.selectById(storedToken.getUserId());
        if (user == null) {
            refreshTokenMapper.revokeById(storedToken.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
        }

        if (!user.getIsActive()) {
            refreshTokenMapper.revokeAllByUserId(user.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
        }

        refreshTokenMapper.revokeById(storedToken.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenResponse(
            accessToken,
            refreshToken,
            TOKEN_PREFIX,
            jwtService.getExpirationSeconds()
        );
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String rawToken = extractRefreshToken(request);
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken storedToken = refreshTokenMapper.selectByTokenHash(tokenHash);
        if (storedToken == null || storedToken.getRevokedAt() != null) {
            return;
        }

        refreshTokenMapper.revokeById(storedToken.getId());
    }

    private String extractRefreshToken(RefreshRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }
        return request.getRefreshToken().trim();
    }
}
