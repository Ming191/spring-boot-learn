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

    /**
     * Enforces JWT-based authentication and authorization for non-public requests, enriches authorized requests with internal user headers, and writes a JSON error response on failure.
     *
     * <p>Behavior:
     * - Removes internal forwarding headers from the incoming request.
     * - Bypasses authentication for public paths.
     * - Validates the `Authorization` header and extracts required JWT claims (`subject`, `username`, `role`).
     * - Denies access if token or required claims are missing or invalid, or if the role is not authorized for the request.
     * - On success, adds `X-User-Id`, `X-Username`, and `X-Role` headers and forwards the request.</p>
     *
     * @param exchange the current server web exchange
     * @param chain the gateway filter chain to forward the request to
     * @return a Mono signaling completion of the filter's processing of the exchange; on success the request is forwarded, on failure an error response is written
     */
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

    /**
     * Removes internal authentication headers from the request and returns a mutated exchange.
     *
     * @param exchange the original server web exchange
     * @return the mutated ServerWebExchange whose request has had internal headers removed
     */
    private ServerWebExchange stripInternalHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .headers(headers -> INTERNAL_HEADERS.forEach(headers::remove))
            .build();

        return exchange.mutate().request(request).build();
    }

    /**
     * Defines the filter's execution precedence among other filters.
     *
     * @return the order value (-1) giving this filter higher-than-default precedence
     */
    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Send a 401 UNAUTHORIZED JSON error response using the provided message.
     *
     * @param exchange the current server exchange used to build and send the response
     * @param message  human-readable error message to include in the JSON body
     * @return         a Mono that completes when the error response has been written
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    /**
     * Sends a JSON 403 Forbidden error response with the given message.
     *
     * @param exchange the current server web exchange
     * @param message the error message to include in the response body
     * @return a Mono that completes when the error response has been written
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    /**
     * Writes a JSON-formatted gateway error response to the exchange's HTTP response.
     *
     * The method sets the response status and Content-Type, constructs a GatewayErrorResponse
     * containing the provided code and message (including timestamp, numeric status and request path),
     * serializes it to JSON, and writes the bytes to the response body. If JSON serialization fails,
     * the response is completed without a body.
     *
     * @param exchange the current server web exchange whose response will be written
     * @param status   the HTTP status to set on the response
     * @param code     an internal error code to include in the JSON payload (e.g. "UNAUTHORIZED")
     * @param message  a human-readable error message to include in the JSON payload
     * @return a Mono that completes when the response write (or response completion on error) finishes
     */
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
