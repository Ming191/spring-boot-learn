package vn.amela.gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.amela.gateway.configuration.PreAuthRateLimitProperties;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreAuthenticationRateLimiterFilterTest {

    private static final String IP_KEY = "ip:127.0.0.1";

    private ObjectMapper objectMapper;
    private RedisRateLimiter redisRateLimiter;
    private PreAuthenticationRateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisRateLimiter = mock(RedisRateLimiter.class);
        PreAuthRateLimitProperties properties = new PreAuthRateLimitProperties();
        KeyResolver keyResolver = exchange -> Mono.just(IP_KEY);

        filter = new PreAuthenticationRateLimiterFilter(
            redisRateLimiter,
            keyResolver,
            properties,
            objectMapper
        );
    }

    @Test
    @DisplayName("limited request returns JSON 429 before auth")
    void limitedRequestReturns429BeforeAuth() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
        );
        CapturingChain chain = new CapturingChain();

        when(redisRateLimiter.isAllowed("pre-auth", IP_KEY))
            .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of())));

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.get("code").asText()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body.get("message").asText()).isEqualTo("Too many requests");
        assertThat(body.get("path").asText()).isEqualTo("/api/employees");
    }

    @Test
    @DisplayName("allowed request continues to authentication filter")
    void allowedRequestContinues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
        );
        CapturingChain chain = new CapturingChain();

        when(redisRateLimiter.isAllowed("pre-auth", IP_KEY))
            .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("rate limiter failure fails open")
    void rateLimiterFailureFailsOpen() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
        );
        CapturingChain chain = new CapturingChain();

        when(redisRateLimiter.isAllowed("pre-auth", IP_KEY))
            .thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private static class CapturingChain implements GatewayFilterChain {

        private final AtomicReference<ServerWebExchange> exchange = new AtomicReference<>();

        @Override
        public Mono<Void> filter(@NonNull ServerWebExchange exchange) {
            this.exchange.set(exchange);
            return Mono.empty();
        }

        private ServerWebExchange exchange() {
            return exchange.get();
        }
    }
}
