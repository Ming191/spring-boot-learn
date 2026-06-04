package vn.amela.leaveservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import vn.amela.leaveservice.exception.InvalidRequestException;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-Role");
        String username = request.getHeader("X-Username");

        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("Missing X-User-Id header");
        }

        if (role == null || role.isBlank()) {
            throw new InvalidRequestException("Missing X-Role header");
        }

        try {
            return new CurrentUser(username, Long.parseLong(userId), role);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid X-User-Id header");
        }
    }
}
