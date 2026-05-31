package vn.amela.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import vn.amela.gateway.dto.response.GatewayErrorResponse;

import java.time.Instant;
import java.util.Collections;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<GatewayErrorResponse> fallback(
        @PathVariable String service,
        ServerWebExchange exchange
    ) {
        GatewayErrorResponse response = new GatewayErrorResponse(
            Instant.now().toString(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "SERVICE_UNAVAILABLE",
            service + " is temporarily unavailable",
            exchange.getRequest().getPath().value(),
            Collections.emptyList()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
