package vn.amela.employeeservice.client;

import org.springframework.stereotype.Component;
import vn.amela.employeeservice.dto.response.AuthUserResponse;
import vn.amela.employeeservice.exception.ServiceUnavailableException;

@Component
public class AuthUserClientFallback implements AuthUserClient {
    @Override
    public AuthUserResponse findByUsernameOrEmail(String usernameOrEmail) {
        throw new ServiceUnavailableException("Auth service is currently unavailable. Please try again later.");
    }
}
