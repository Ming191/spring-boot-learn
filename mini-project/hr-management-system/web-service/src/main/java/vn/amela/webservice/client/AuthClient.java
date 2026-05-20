package vn.amela.webservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import vn.amela.webservice.dto.request.LoginForm;
import vn.amela.webservice.dto.request.RefreshTokenRequest;
import vn.amela.webservice.dto.request.RegisterForm;
import vn.amela.webservice.dto.response.TokenResponse;
import vn.amela.webservice.dto.response.UserResponse;

import java.util.Map;

@FeignClient(name = "auth-service", contextId = "authClient", path = "/api/auth")
public interface AuthClient {

    @PostMapping("/login")
    TokenResponse login(@RequestBody LoginForm request);

    @PostMapping("/register")
    UserResponse register(@RequestBody RegisterForm request);

    @PostMapping("/refresh")
    TokenResponse refresh(@RequestBody RefreshTokenRequest request);

    @GetMapping("/me")
    Map<String, Object> me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/logout")
    void logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestBody RefreshTokenRequest request
    );
}
