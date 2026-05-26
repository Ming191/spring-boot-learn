package vn.amela.gateway.security;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.amela.gateway.security.authorization.GatewayAuthorizationProperties;
import vn.amela.gateway.security.authorization.GatewayAuthorizationService;

import java.net.URI;
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
    @DisplayName("logout without token passes through so auth service can clear cookies")
    void logoutWithoutTokenPassesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/logout")
                .header("X-User-Id", "1")
                .header("X-Username", "attacker")
                .header("X-Role", "HR")
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        HttpHeaders headers = chain.exchange().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst("X-Username")).isNull();
        assertThat(headers.getFirst("X-Role")).isNull();
    }

    @Test
    @DisplayName("employee role cannot access HR-only employee UI")
    void employeeRoleCannotAccessHrOnlyEmployeeUi() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("path").asText()).isEqualTo("/employees");
    }

    @Test
    @DisplayName("employee role can access own leave UI")
    void employeeRoleCanAccessOwnLeaveUi() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/leaves/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("employee role cannot access all leave UI")
    void employeeRoleCannotAccessAllLeaveUi() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/leaves")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("path").asText()).isEqualTo("/leaves");
    }

    @Test
    @DisplayName("HR role can access all leave UI")
    void hrRoleCanAccessAllLeaveUi() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/leaves")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "HR"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("authenticated HR visiting /login redirects to /employees")
    void authenticatedHrVisitingLoginPageIsRedirected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "HR"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/employees"));
    }

    @Test
    @DisplayName("authenticated EMPLOYEE visiting /login redirects to /leaves/my")
    void authenticatedEmployeeVisitingLoginPageIsRedirected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/leaves/my"));
    }

    @Test
    @DisplayName("authenticated HR visiting /register redirects to /employees")
    void authenticatedHrVisitingRegisterPageIsRedirected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "HR"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/employees"));
    }

    @Test
    @DisplayName("authenticated EMPLOYEE visiting /register redirects to /leaves/my")
    void authenticatedEmployeeVisitingRegisterPageIsRedirected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE"))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/leaves/my"));
    }

    @Test
    @DisplayName("protected path with unsupported JWT algorithm returns JSON 401")
    void protectedPathWithUnsupportedJwtAlgorithmReturns401() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(AUDIENCE, "EMPLOYEE", Jwts.SIG.HS384))
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.exchange()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("message").asText()).isEqualTo("Invalid token");
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
        return accessToken(audience, role, Jwts.SIG.HS256);
    }

    private static String accessToken(String audience, String role, io.jsonwebtoken.security.MacAlgorithm algorithm) {
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
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), algorithm)
            .compact();
    }

    private static GatewayAuthorizationService authorizationService() {
        GatewayAuthorizationProperties properties = new GatewayAuthorizationProperties();
        properties.setPublicPaths(List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/login",
            "/register",
            "/logout",
            "/auth-assets/**",
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
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties departmentMutationRule = authorizationRule(
            "/api/departments/**",
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties employeesUiRule = authorizationRule(
            "/employees",
            List.of(HttpMethod.GET),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties leavesMyUiRule = authorizationRule(
            "/leaves/my",
            List.of(HttpMethod.GET),
            List.of("EMPLOYEE")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties leavesNewUiRule = authorizationRule(
            "/leaves/new",
            List.of(HttpMethod.GET, HttpMethod.POST),
            List.of("HR", "EMPLOYEE")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties leavesIndexUiRule = authorizationRule(
            "/leaves",
            List.of(HttpMethod.GET),
            List.of("HR")
        );
        GatewayAuthorizationProperties.AuthorizationRuleProperties leavesUiRule = authorizationRule(
            "/leaves/**",
            List.of(HttpMethod.GET),
            List.of("HR", "EMPLOYEE")
        );

        properties.setAuthorizationRules(List.of(
            authAdminRule,
            employeesUiRule,
            leavesMyUiRule,
            leavesNewUiRule,
            leavesIndexUiRule,
            leavesUiRule,
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
