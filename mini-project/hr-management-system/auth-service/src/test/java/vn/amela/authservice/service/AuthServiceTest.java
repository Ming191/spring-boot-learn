package vn.amela.authservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register trims data, hashes password, creates active employee")
    void registerCreatesEmployee() {
        RegisterRequest request = registerRequest(" emp_test ", "EMP_TEST@Company.COM ", " Employee Test ");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return null;
        }).when(userMapper).insert(any(User.class));

        UserResponse response = authService.register(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUsername()).isEqualTo("emp_test");
        assertThat(response.getEmail()).isEqualTo("emp_test@company.com");
        assertThat(response.getFullName()).isEqualTo("Employee Test");
        assertThat(response.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(response.getIsActive()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("register rejects duplicate username")
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = registerRequest("emp_test", "emp_test@company.com", "Employee Test");
        when(userMapper.selectByUserName("emp_test")).thenReturn(activeUser());

        assertStatus(
            () -> authService.register(request),
            HttpStatus.CONFLICT,
            "Username already exists"
        );

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("register rejects duplicate email")
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = registerRequest("emp_test", "emp_test@company.com", "Employee Test");
        when(userMapper.selectByEmail("emp_test@company.com")).thenReturn(activeUser());

        assertStatus(
            () -> authService.register(request),
            HttpStatus.CONFLICT,
            "Email already exists"
        );

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("login accepts trimmed lowercase email and returns access plus refresh token")
    void loginReturnsTokens() {
        LoginRequest request = loginRequest(" EMP@Company.COM ", "password123");
        User user = activeUser();
        when(userMapper.selectByUserNameOrEmail("emp@company.com")).thenReturn(user);
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(refreshTokenMapper.revokeAllByUserId(user.getId())).thenReturn(1);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        TokenResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(refreshTokenMapper).revokeAllByUserId(user.getId());
    }

    @Test
    @DisplayName("login rejects unknown user")
    void loginRejectsUnknownUser() {
        LoginRequest request = loginRequest("missing", "password123");

        assertStatus(
            () -> authService.login(request),
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password"
        );
    }

    @Test
    @DisplayName("login rejects invalid password")
    void loginRejectsInvalidPassword() {
        LoginRequest request = loginRequest("emp_test", "wrong");
        User user = activeUser();
        when(userMapper.selectByUserNameOrEmail("emp_test")).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertStatus(
            () -> authService.login(request),
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password"
        );

        verify(refreshTokenMapper, never()).revokeAllByUserId(user.getId());
    }

    @Test
    @DisplayName("login rejects inactive user")
    void loginRejectsInactiveUser() {
        LoginRequest request = loginRequest("emp_test", "password123");
        User user = activeUser();
        user.setIsActive(false);
        when(userMapper.selectByUserNameOrEmail("emp_test")).thenReturn(user);
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

        assertStatus(
            () -> authService.login(request),
            HttpStatus.FORBIDDEN,
            "User is inactive"
        );

        verify(refreshTokenMapper, never()).revokeAllByUserId(user.getId());
    }

    @Test
    @DisplayName("refresh rotates a valid token")
    void refreshRotatesValidToken() {
        RefreshRequest request = refreshRequest("raw-refresh");
        User user = activeUser();
        RefreshToken storedToken = refreshToken(20L, user.getId(), LocalDateTime.now().plusDays(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(refreshTokenMapper.revokeById(storedToken.getId())).thenReturn(1);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        TokenResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(refreshTokenMapper).revokeById(storedToken.getId());
    }

    @Test
    @DisplayName("refresh rejects blank token")
    void refreshRejectsBlankToken() {
        assertStatus(
            () -> authService.refresh(refreshRequest("  ")),
            HttpStatus.BAD_REQUEST,
            "Refresh token is required"
        );
    }

    @Test
    @DisplayName("refresh rejects unknown token")
    void refreshRejectsUnknownToken() {
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.UNAUTHORIZED,
            "Invalid refresh token"
        );
    }

    @Test
    @DisplayName("refresh revokes all user tokens when a reused token is detected")
    void refreshRejectsReusedToken() {
        RefreshToken storedToken = refreshToken(20L, 1L, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.UNAUTHORIZED,
            "Invalid refresh token"
        );

        verify(refreshTokenMapper).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("refresh revokes expired token")
    void refreshRejectsExpiredToken() {
        RefreshToken storedToken = refreshToken(20L, 1L, LocalDateTime.now().minusSeconds(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.UNAUTHORIZED,
            "Invalid refresh token"
        );

        verify(refreshTokenMapper).revokeById(20L);
    }

    @Test
    @DisplayName("refresh revokes token when user no longer exists")
    void refreshRejectsMissingUser() {
        RefreshToken storedToken = refreshToken(20L, 1L, LocalDateTime.now().plusDays(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.UNAUTHORIZED,
            "Invalid refresh token"
        );

        verify(refreshTokenMapper).revokeById(20L);
    }

    @Test
    @DisplayName("refresh revokes all tokens for inactive user")
    void refreshRejectsInactiveUser() {
        User user = activeUser();
        user.setIsActive(false);
        RefreshToken storedToken = refreshToken(20L, user.getId(), LocalDateTime.now().plusDays(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);
        when(userMapper.selectById(user.getId())).thenReturn(user);

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.FORBIDDEN,
            "User is inactive"
        );

        verify(refreshTokenMapper).revokeAllByUserId(user.getId());
    }

    @Test
    @DisplayName("refresh rejects concurrent reuse when revoke update loses the race")
    void refreshRejectsLostRotationRace() {
        User user = activeUser();
        RefreshToken storedToken = refreshToken(20L, user.getId(), LocalDateTime.now().plusDays(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(refreshTokenMapper.revokeById(storedToken.getId())).thenReturn(0);

        assertStatus(
            () -> authService.refresh(refreshRequest("raw-refresh")),
            HttpStatus.UNAUTHORIZED,
            "Invalid refresh token"
        );

        verify(refreshTokenMapper).revokeAllByUserId(user.getId());
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("logout revokes an active refresh token")
    void logoutRevokesToken() {
        RefreshToken storedToken = refreshToken(20L, 1L, LocalDateTime.now().plusDays(1), null);
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");
        when(refreshTokenMapper.selectByTokenHash("hash")).thenReturn(storedToken);

        authService.logout(refreshRequest("raw-refresh"));

        verify(refreshTokenMapper).revokeById(20L);
    }

    @Test
    @DisplayName("logout is idempotent for missing or already revoked tokens")
    void logoutIsIdempotent() {
        when(refreshTokenService.hashToken("raw-refresh")).thenReturn("hash");

        authService.logout(refreshRequest("raw-refresh"));

        verify(refreshTokenMapper, never()).revokeById(any());
    }

    private static RegisterRequest registerRequest(String username, String email, String fullName) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("password123");
        request.setEmail(email);
        request.setFullName(fullName);
        return request;
    }

    private static LoginRequest loginRequest(String usernameOrEmail, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(usernameOrEmail);
        request.setPassword(password);
        return request;
    }

    private static RefreshRequest refreshRequest(String refreshToken) {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private static User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("emp");
        user.setEmail("emp@company.com");
        user.setPassword("encoded-password");
        user.setFullName("Employee");
        user.setRole(Role.EMPLOYEE);
        user.setIsActive(true);
        return user;
    }

    private static RefreshToken refreshToken(
        Long id,
        Long userId,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash("hash");
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevokedAt(revokedAt);
        return refreshToken;
    }

    private static void assertStatus(Runnable action, HttpStatus status, String reason) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                assertThat(exception.getStatusCode()).isEqualTo(status);
                assertThat(exception.getReason()).isEqualTo(reason);
            });
    }
}
