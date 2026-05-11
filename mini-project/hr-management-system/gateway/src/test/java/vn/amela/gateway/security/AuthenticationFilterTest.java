package vn.amela.gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.amela.gateway.security.authorization.GatewayAuthorizationProperties;
import vn.amela.gateway.security.authorization.GatewayAuthorizationService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long";
    private static final String ISSUER = "hr-auth-service";
    private static final String AUDIENCE = "hr-management-system";

    private ObjectMapper objectMapper;
    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "issuer", ISSUER);
        ReflectionTestUtils.setField(jwtService, "audience", AUDIENCE);
        jwtService.init();

        filter = new AuthenticationFilter(jwtService, objectMapper, authorizationService());
    }

    @Test
    @DisplayName("protected path without token returns JSON 401 at gateway")
    void protectedPathWithoutTokenReturns401() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("Authentication is required");
        assertThat(body.get("path").asText()).isEqualTo("/api/employees");
    }

    @Test
    @DisplayName("public auth path passes through and strips forged identity headers")
    void publicAuthPathPassesThroughAndStripsIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/auth/login")
                .header("X-User-Id", "1")
                .header("X-Username", "attacker")
                .header("X-Role", "HR")
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        HttpHeaders headers = chain.exchange().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst("X-Username")).isNull();
        assertThat(headers.getFirst("X-Role")).isNull();
    }

    @Test
    @DisplayName("OPTIONS preflight passes through without token")
    void optionsPreflightPassesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/employees")
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("protected path with valid token forwards trusted identity headers")
    void protectedPathWithValidTokenForwardsIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE))
                .header("X-User-Id", "999")
                .header("X-Username", "forged")
                .header("X-Role", "HR")
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        HttpHeaders headers = chain.exchange().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("1");
        assertThat(headers.getFirst("X-Username")).isEqualTo("emp");
        assertThat(headers.getFirst("X-Role")).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("employee role cannot access HR-only employee mutation path")
    void employeeRoleCannotAccessHrOnlyEmployeeMutationPath() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
        assertThat(body.get("message").asText()).isEqualTo("Access denied");
        assertThat(body.get("path").asText()).isEqualTo("/api/employees");
    }

    @Test
    @DisplayName("HR role can access HR-only employee mutation path")
    void hrRoleCanAccessHrOnlyEmployeeMutationPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "HR"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();

        HttpHeaders headers = chain.exchange().getRequest().getHeaders();
        assertThat(headers.getFirst("X-Role")).isEqualTo("HR");
    }

    @Test
    @DisplayName("employee role cannot access auth admin path")
    void employeeRoleCannotAccessAuthAdminPath() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/auth/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("protected path with wrong audience returns JSON 401")
    void protectedPathWithWrongAudienceReturns401() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("other-system"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("message").asText()).isEqualTo("Invalid token");
    }

    private static String accessToken(String audience) {
        return accessToken(audience, "EMPLOYEE");
    }

    private static String accessToken(String audience, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900_000L);

        return Jwts.builder()
            .subject("1")
            .claim("username", "emp")
            .claim("role", role)
            .audience().add(audience).and()
            .issuer(AuthenticationFilterTest.ISSUER)
            .issuedAt(now)
            .notBefore(now)
            .expiration(expiry)
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private static GatewayAuthorizationService authorizationService() {
        GatewayAuthorizationProperties properties = new GatewayAuthorizationProperties();
        properties.setPublicPaths(List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator/health",
            "/actuator/info"
        ));

        GatewayAuthorizationProperties.AuthorizationRuleProperties authAdminRule = authorizationRule(
            "/api/auth/admin/**",
            List.of(),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties employeeMutationRule = authorizationRule(
            "/api/employees/**",
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties departmentMutationRule = authorizationRule(
            "/api/departments/**",
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
            List.of("HR")
        );

        properties.setAuthorizationRules(List.of(
            authAdminRule,
            employeeMutationRule,
            departmentMutationRule
        ));

        return new GatewayAuthorizationService(properties);
    }

    private static GatewayAuthorizationProperties.AuthorizationRuleProperties authorizationRule(
        String path,
        List<HttpMethod> methods,
        List<String> roles
    ) {
        GatewayAuthorizationProperties.AuthorizationRuleProperties rule =
            new GatewayAuthorizationProperties.AuthorizationRuleProperties();

        rule.setPath(path);
        rule.setMethods(methods);
        rule.setRoles(roles);

        return rule;
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
