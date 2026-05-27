package vn.amela.leaveservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-Role");
        String username = request.getHeader("X-Username");

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Missing X-Role header");
        }

        return new CurrentUser(username, Long.parseLong(userId), role);
    }
}
