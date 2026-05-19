package vn.amela.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import vn.amela.gateway.dto.response.GatewayErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    @DisplayName("fallback returns service unavailable JSON response")
    void fallbackReturnsServiceUnavailable() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/fallback/employee-service")
        );

        ResponseEntity<GatewayErrorResponse> response = controller.fallback(
            "employee-service",
            exchange
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().code()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isEqualTo("employee-service is temporarily unavailable");
        assertThat(response.getBody().path()).isEqualTo("/fallback/employee-service");
        assertThat(response.getBody().errors()).isEmpty();
    }
}
