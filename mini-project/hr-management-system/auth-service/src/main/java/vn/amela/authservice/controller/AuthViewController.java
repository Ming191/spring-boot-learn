package vn.amela.authservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import vn.amela.authservice.dto.request.LoginRequest;
import vn.amela.authservice.dto.request.RefreshRequest;
import vn.amela.authservice.dto.request.RegisterRequest;
import vn.amela.authservice.dto.response.TokenResponse;
import vn.amela.authservice.exception.AuthException;
import vn.amela.authservice.service.AuthService;

import java.net.URI;
import java.time.Duration;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private static final String ACCESS_TOKEN_COOKIE = "HR_ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "HR_REFRESH_TOKEN";
    private static final String COOKIE_PATH = "/";

    private final AuthService authService;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @GetMapping("/login")
    public String loginForm(Model model) {
        if (!model.containsAttribute("login")) {
            model.addAttribute("login", new LoginRequest());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public Object login(
        @Valid @ModelAttribute("login") LoginRequest request,
        BindingResult bindingResult,
        HttpServletResponse response,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            TokenResponse tokenResponse = authService.login(request);
            addAuthCookies(response, tokenResponse);
            return redirectTo("/employees");
        } catch (AuthException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("register")) {
            model.addAttribute("register", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public Object register(
        @Valid @ModelAttribute("register") RegisterRequest request,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return redirectTo("/login?registered");
        } catch (AuthException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken(refreshToken);
            authService.logout(request);
        }

        clearAuthCookies(response);
        return redirectTo("/login?logout");
    }

    private ResponseEntity<Void> redirectTo(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .location(URI.create(location))
            .build();
    }

    private void addAuthCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        addCookie(
            response,
            ACCESS_TOKEN_COOKIE,
            tokenResponse.getAccessToken(),
            Duration.ofSeconds(tokenResponse.getExpiresIn())
        );
        addCookie(
            response,
            REFRESH_TOKEN_COOKIE,
            tokenResponse.getRefreshToken(),
            Duration.ofDays(refreshExpirationDays)
        );
    }

    private void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
