package vn.amela.employeeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.amela.employeeservice.dto.response.AuthUserResponse;

@FeignClient(name = "auth-service", fallback = AuthUserClientFallback.class)
public interface AuthUserClient {

    @GetMapping("/internal/users/by-username-or-email/{usernameOrEmail}")
    AuthUserResponse findByUsernameOrEmail(@PathVariable String usernameOrEmail);
}
