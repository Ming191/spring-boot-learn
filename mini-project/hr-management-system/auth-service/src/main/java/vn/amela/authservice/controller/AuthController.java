package vn.amela.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.amela.authservice.dto.request.LoginRequest;
import vn.amela.authservice.dto.request.RefreshRequest;
import vn.amela.authservice.dto.request.RegisterRequest;
import vn.amela.authservice.dto.response.CurrentUserResponse;
import vn.amela.authservice.dto.response.TokenResponse;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.security.AuthenticatedUser;
import vn.amela.authservice.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return CurrentUserResponse.builder()
            .userId(user.userId())
            .username(user.username())
            .role(user.role())
            .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
        @Valid @RequestBody RefreshRequest request
    ) {
        return ResponseEntity.ok(
            authService.refresh(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
