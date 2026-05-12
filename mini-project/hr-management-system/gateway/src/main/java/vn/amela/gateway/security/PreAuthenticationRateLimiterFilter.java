package vn.amela.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.amela.gateway.configuration.PreAuthRateLimitProperties;
import vn.amela.gateway.dto.response.GatewayErrorResponse;

import java.time.Instant;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class PreAuthenticationRateLimiterFilter implements GlobalFilter, Ordered {

    private static final String PRE_AUTH_ROUTE_ID = "pre-auth";
    private static final String TOO_MANY_REQUESTS = "Too many requests";

    private final RedisRateLimiter redisRateLimiter;
    @Qualifier("ipKeyResolver")
    private final KeyResolver keyResolver;
    private final PreAuthRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void configureRateLimiter() {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config()
            .setReplenishRate(properties.getReplenishRate())
            .setBurstCapacity(properties.getBurstCapacity())
            .setRequestedTokens(properties.getRequestedTokens());

        redisRateLimiter.getConfig().put(PRE_AUTH_ROUTE_ID, config);
    }

    @Override
    public Mono<Void> filter(
        @NonNull ServerWebExchange exchange,
        @NonNull GatewayFilterChain chain
    ) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        return keyResolver.resolve(exchange)
            .filter(key -> !key.isBlank())
            .defaultIfEmpty("ip:unknown")
            .flatMap(key -> redisRateLimiter.isAllowed(PRE_AUTH_ROUTE_ID, key))
            .flatMap(response -> {
                if (response.isAllowed()) {
                    return chain.filter(exchange);
                }

                return tooManyRequests(exchange);
            })
            .onErrorResume(exception -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -2;
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse errorResponse = new GatewayErrorResponse(
            Instant.now().toString(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "TOO_MANY_REQUESTS",
            TOO_MANY_REQUESTS,
            exchange.getRequest().getURI().getPath(),
            Collections.emptyList()
        );

        try {
            byte[] body = objectMapper.writeValueAsBytes(errorResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (JsonProcessingException exception) {
            return response.setComplete();
        }
    }
}
