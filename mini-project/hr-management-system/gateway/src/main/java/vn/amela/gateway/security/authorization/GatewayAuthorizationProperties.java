package vn.amela.gateway.security.authorization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayAuthorizationProperties {

    private List<String> publicPaths = new ArrayList<>();
    private List<AuthorizationRuleProperties> authorizationRules = new ArrayList<>();

    @Getter
    @Setter
    public static class AuthorizationRuleProperties {

        private String path;
        private List<HttpMethod> methods = new ArrayList<>();
        private List<String> roles = new ArrayList<>();
    }
}
