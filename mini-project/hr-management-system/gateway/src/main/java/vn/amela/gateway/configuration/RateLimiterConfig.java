package vn.amela.gateway.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .orElse("unknown");

            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId == null || userId.isBlank()) {
                return Mono.just("user:anonymous");
            }

            return Mono.just("user:" + userId);
        };
    }
}
