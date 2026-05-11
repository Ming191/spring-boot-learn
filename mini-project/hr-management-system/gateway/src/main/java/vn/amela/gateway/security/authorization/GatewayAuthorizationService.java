package vn.amela.gateway.security.authorization;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayAuthorizationService {

    private static final PathPatternParser PATH_PATTERN_PARSER = PathPatternParser.defaultInstance;

    private final GatewayAuthorizationProperties properties;

    public boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        return request.getMethod() == HttpMethod.OPTIONS
            || properties.getPublicPaths().stream()
                .anyMatch(publicPath -> matchesPath(publicPath, path));
    }

    public boolean isAuthorized(ServerHttpRequest request, String role) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        return properties.getAuthorizationRules().stream()
            .filter(rule -> matches(rule, method, path))
            .findFirst()
            .map(rule -> allows(rule, role))
            .orElse(true);
    }

    private boolean matches(
        GatewayAuthorizationProperties.AuthorizationRuleProperties rule,
        HttpMethod requestMethod,
        String requestPath
    ) {
        return matchesMethod(rule.getMethods(), requestMethod)
            && matchesPath(rule.getPath(), requestPath);
    }

    private boolean matchesMethod(List<HttpMethod> methods, HttpMethod requestMethod) {
        return methods == null || methods.isEmpty() || methods.contains(requestMethod);
    }

    private boolean matchesPath(String pathPattern, String requestPath) {
        return pathPattern != null
            && !pathPattern.isBlank()
            && PATH_PATTERN_PARSER.parse(pathPattern)
                .matches(PathContainer.parsePath(requestPath));
    }

    private boolean allows(GatewayAuthorizationProperties.AuthorizationRuleProperties rule, String role) {
        return rule.getRoles() != null && rule.getRoles().contains(role);
    }
}
