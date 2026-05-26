package vn.amela.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import vn.amela.gateway.dto.response.GatewayErrorResponse;
import vn.amela.gateway.security.authorization.GatewayAuthorizationService;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLE_HEADER = "X-Role";
    private static final String ACCESS_TOKEN_COOKIE = "HR_ACCESS_TOKEN";
    private static final List<String> INTERNAL_HEADERS = List.of(
        USER_ID_HEADER,
        USERNAME_HEADER,
        ROLE_HEADER
    );
    private static final String LOGIN_PATH = "/login";
    private static final String REGISTER_PATH = "/register";
    private static final String EMPLOYEES_PATH = "/employees";
    private static final String AUTHENTICATION_REQUIRED = "Authentication is required";
    private static final String INVALID_TOKEN = "Invalid token";
    private static final String ACCESS_DENIED = "Access denied";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final GatewayAuthorizationService authorizationService;

    @Override
    @NullMarked
    public Mono<Void> filter(
        ServerWebExchange exchange,
        GatewayFilterChain chain) {

        ServerWebExchange sanitizedExchange = stripInternalHeaders(exchange);
        ServerHttpRequest sanitizedRequest = sanitizedExchange.getRequest();

        if (isAuthPage(sanitizedRequest) && hasValidToken(sanitizedRequest)) {
            return redirect(sanitizedExchange, EMPLOYEES_PATH);
        }

        if (authorizationService.isPublicPath(sanitizedRequest)) {
            return chain.filter(sanitizedExchange);
        }

        String token = resolveToken(sanitizedRequest);
        if (token == null) {
            return unauthorized(sanitizedExchange, AUTHENTICATION_REQUIRED);
        }

        if (token.isBlank()) {
            return unauthorized(sanitizedExchange, INVALID_TOKEN);
        }

        try {
            Claims claims = jwtService.extractClaims(token);

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            if (userId == null ||
                username == null ||
                role == null ||
                userId.isBlank() ||
                username.isBlank() ||
                role.isBlank()) {
                return unauthorized(sanitizedExchange, INVALID_TOKEN);
            }

            if (!authorizationService.isAuthorized(sanitizedRequest, role)) {
                return forbidden(sanitizedExchange, ACCESS_DENIED);
            }

            ServerHttpRequest request = sanitizedRequest.mutate()
                .header(USER_ID_HEADER, userId)
                .header(USERNAME_HEADER, username)
                .header(ROLE_HEADER, role)
                .build();

            return chain.filter(sanitizedExchange.mutate().request(request).build());
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(sanitizedExchange, INVALID_TOKEN);
        }
    }

    private ServerWebExchange stripInternalHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .headers(headers -> INTERNAL_HEADERS.forEach(headers::remove))
            .build();

        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String resolveToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        HttpCookie cookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        return cookie == null ? null : cookie.getValue();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private boolean isAuthPage(ServerHttpRequest request) {
        if (request.getMethod() != HttpMethod.GET) {
            return false;
        }
        String path = request.getURI().getPath();
        return LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path);
    }

    private boolean hasValidToken(ServerHttpRequest request) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            jwtService.extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String location) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().setLocation(URI.create(location));
        return response.setComplete();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse errorResponse = new GatewayErrorResponse(
            Instant.now().toString(),
            status.value(),
            code,
            message,
            exchange.getRequest().getURI().getPath(),
            Collections.emptyList()
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(errorResponse);
            return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body))
            );
        } catch (JacksonException exception) {
            return response.setComplete();
        }
    }

}
