package vn.amela.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.amela.gateway.dto.response.GatewayErrorResponse;
import vn.amela.gateway.security.authorization.GatewayAuthorizationService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLE_HEADER = "X-Role";
    private static final List<String> INTERNAL_HEADERS = List.of(
        USER_ID_HEADER,
        USERNAME_HEADER,
        ROLE_HEADER
    );
    private static final String AUTHENTICATION_REQUIRED = "Authentication is required";
    private static final String INVALID_TOKEN = "Invalid token";
    private static final String ACCESS_DENIED = "Access denied";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final GatewayAuthorizationService authorizationService;

    @Override
    public Mono<Void> filter(
        @NonNull ServerWebExchange exchange,
        @NonNull GatewayFilterChain chain) {

        ServerWebExchange sanitizedExchange = stripInternalHeaders(exchange);
        ServerHttpRequest sanitizedRequest = sanitizedExchange.getRequest();

        if (authorizationService.isPublicPath(sanitizedRequest)) {
            return chain.filter(sanitizedExchange);
        }

        String authHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(sanitizedExchange, AUTHENTICATION_REQUIRED);
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            return unauthorized(sanitizedExchange, INVALID_TOKEN);
        }

        try {
            Claims claims = jwtService.extractClaims(token);

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            if (userId == null || username == null || role == null || userId.isBlank() || username.isBlank() || role.isBlank()) {
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

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", message);
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
        } catch (JsonProcessingException exception) {
            return response.setComplete();
        }
    }

}
