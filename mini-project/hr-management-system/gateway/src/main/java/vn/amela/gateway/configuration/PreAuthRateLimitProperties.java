package vn.amela.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.pre-auth-rate-limit")
public class PreAuthRateLimitProperties {

    private boolean enabled = true;
    private int replenishRate = 30;
    private long burstCapacity = 60;
    private int requestedTokens = 1;
}
