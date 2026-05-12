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

    /**
     * Determines whether the given HTTP request is considered public and does not require authorization.
     *
     * @param request the incoming HTTP request to evaluate
     * @return `true` if the request is public (the HTTP method is `OPTIONS` or the request path matches a configured public path pattern), `false` otherwise
     */
    public boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        return request.getMethod() == HttpMethod.OPTIONS
            || properties.getPublicPaths().stream()
                .anyMatch(publicPath -> matchesPath(publicPath, path));
    }

    /**
     * Determine whether a request with the given role is permitted by the first matching authorization rule.
     *
     * Evaluates configured authorization rules in order, selects the first rule whose method and path constraints
     * match the request, and allows the request only if that rule's allowed roles contain the provided role.
     *
     * @param request the incoming HTTP request to evaluate (method and path are inspected)
     * @param role the role to verify against the matched rule's allowed roles
     * @return {@code true} if the request is permitted (either no rule matches or the matched rule allows the role), {@code false} otherwise
     */
    public boolean isAuthorized(ServerHttpRequest request, String role) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        return properties.getAuthorizationRules().stream()
            .filter(rule -> matches(rule, method, path))
            .findFirst()
            .map(rule -> allows(rule, role))
            .orElse(true);
    }

    /**
     * Determines whether the authorization rule applies to the given request method and path.
     *
     * @param rule the authorization rule to evaluate
     * @param requestMethod the HTTP method of the incoming request
     * @param requestPath the request URI path to match against the rule's path pattern
     * @return `true` if the rule's method constraints (if any) include `requestMethod` and the rule's path pattern matches `requestPath`, `false` otherwise
     */
    private boolean matches(
        GatewayAuthorizationProperties.AuthorizationRuleProperties rule,
        HttpMethod requestMethod,
        String requestPath
    ) {
        return matchesMethod(rule.getMethods(), requestMethod)
            && matchesPath(rule.getPath(), requestPath);
    }

    /**
     * Determines whether the provided HTTP request method is allowed by the given method list.
     *
     * @param methods the allowed HTTP methods, or null/empty to indicate no method restriction
     * @param requestMethod the HTTP method of the incoming request
     * @return `true` if `methods` is null, empty, or contains `requestMethod`, `false` otherwise
     */
    private boolean matchesMethod(List<HttpMethod> methods, HttpMethod requestMethod) {
        return methods == null || methods.isEmpty() || methods.contains(requestMethod);
    }

    /**
     * Checks if the specified request path matches the provided path pattern.
     *
     * @param pathPattern the path pattern to match against; a null or blank pattern does not match
     * @param requestPath the request URI path to test
     * @return `true` if the pattern is non-null, not blank, and matches the request path, `false` otherwise
     */
    private boolean matchesPath(String pathPattern, String requestPath) {
        return pathPattern != null
            && !pathPattern.isBlank()
            && PATH_PATTERN_PARSER.parse(pathPattern)
                .matches(PathContainer.parsePath(requestPath));
    }

    /**
     * Determines whether the given authorization rule allows the specified role.
     *
     * @param rule the authorization rule to check
     * @param role the role to verify against the rule's allowed roles
     * @return `true` if the rule's allowed roles contains the role, `false` otherwise
     */
    private boolean allows(GatewayAuthorizationProperties.AuthorizationRuleProperties rule, String role) {
        return rule.getRoles() != null && rule.getRoles().contains(role);
    }
}
