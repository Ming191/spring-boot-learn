package vn.amela.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
import vn.amela.authservice.exception.DuplicateResourceException;
import vn.amela.authservice.exception.InactiveUserException;
import vn.amela.authservice.exception.InvalidCredentialsException;
import vn.amela.authservice.exception.InvalidRequestException;
import vn.amela.authservice.exception.InvalidTokenException;
import vn.amela.authservice.exception.TokenExpiredException;
import vn.amela.authservice.exception.TokenRevokedException;
import vn.amela.authservice.mapper.RefreshTokenMapper;
import vn.amela.authservice.mapper.UserMapper;
import vn.amela.authservice.security.JwtService;

import java.time.LocalDateTime;
import java.util.Locale;

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

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Role role = Role.EMPLOYEE;

        if (userMapper.selectByUserName(username) != null) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userMapper.selectByEmail(email) != null) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        user.setIsActive(true);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw duplicateFromConstraint(exception);
        }

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .build();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {

        String usernameOrEmail = normalizeUsernameOrEmail(request.getUsernameOrEmail());
        User user = userMapper.selectByUserNameOrEmail(usernameOrEmail);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.getIsActive()) {
            throw new InactiveUserException();
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
            throw new InvalidTokenException(INVALID_REFRESH_TOKEN);
        }

        if (storedToken.getRevokedAt() != null) {
            refreshTokenMapper.revokeAllByUserId(storedToken.getUserId());
            throw new TokenRevokedException(INVALID_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (storedToken.getExpiresAt() == null || storedToken.getExpiresAt().isBefore(now)) {
            refreshTokenMapper.revokeById(storedToken.getId());
            throw new TokenExpiredException(INVALID_REFRESH_TOKEN);
        }

        User user = userMapper.selectById(storedToken.getUserId());
        if (user == null) {
            refreshTokenMapper.revokeById(storedToken.getId());
            throw new InvalidTokenException(INVALID_REFRESH_TOKEN);
        }

        if (!user.getIsActive()) {
            refreshTokenMapper.revokeAllByUserId(user.getId());
            throw new InactiveUserException();
        }

        int revokedTokenCount = refreshTokenMapper.revokeById(storedToken.getId());
        if (revokedTokenCount != 1) {
            refreshTokenMapper.revokeAllByUserId(storedToken.getUserId());
            throw new TokenRevokedException(INVALID_REFRESH_TOKEN);
        }

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
            throw new InvalidRequestException("Refresh token is required");
        }
        return request.getRefreshToken().trim();
    }

    private String normalizeUsernameOrEmail(String usernameOrEmail) {
        String value = usernameOrEmail.trim();
        if (value.contains("@")) {
            return value.toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private DuplicateResourceException duplicateFromConstraint(DuplicateKeyException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null) {
            String normalizedMessage = message.toLowerCase(Locale.ROOT);
            if (normalizedMessage.contains("username")) {
                return new DuplicateResourceException("Username already exists");
            }
            if (normalizedMessage.contains("email")) {
                return new DuplicateResourceException("Email already exists");
            }
        }
        return new DuplicateResourceException("Username or email already exists");
    }
}
