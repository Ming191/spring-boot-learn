package vn.amela.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.amela.authservice.dto.request.LoginRequest;
import vn.amela.authservice.dto.request.RegisterRequest;
import vn.amela.authservice.dto.response.TokenResponse;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.mapper.UserMapper;
import vn.amela.authservice.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_PREFIX = "Bearer";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
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

        String accessToken = jwtService.generateAccessToken(user);

        return new TokenResponse(
                accessToken,
                TOKEN_PREFIX,
                jwtService.getExpirationSeconds()
        );
    }
}
