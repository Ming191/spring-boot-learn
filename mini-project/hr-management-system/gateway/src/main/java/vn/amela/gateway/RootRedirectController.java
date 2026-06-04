package vn.amela.gateway;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import vn.amela.gateway.security.JwtService;

import java.net.URI;

@RestController
public class RootRedirectController {

    private static final String ACCESS_TOKEN_COOKIE = "HR_ACCESS_TOKEN";
    private static final String EMPLOYEES_PATH = "/employees";
    private static final String LOGIN_PATH = "/login";
    private static final String MY_LEAVE_PATH = "/leaves/my";

    private final JwtService jwtService;

    public RootRedirectController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/")
    public Mono<Void> redirectHome(ServerHttpRequest request, ServerHttpResponse response) {
        String location = LOGIN_PATH;
        String token = resolveToken(request);

        if (token != null && !token.isBlank()) {
            try {
                String role = jwtService.extractClaims(token).get("role", String.class);
                location = "HR".equals(role) ? EMPLOYEES_PATH : MY_LEAVE_PATH;
            } catch (RuntimeException ignored) {
                location = LOGIN_PATH;
            }
        }

        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().setLocation(URI.create(location));
        return response.setComplete();
    }

    private String resolveToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        HttpCookie cookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        return cookie == null ? null : cookie.getValue();
    }
}
