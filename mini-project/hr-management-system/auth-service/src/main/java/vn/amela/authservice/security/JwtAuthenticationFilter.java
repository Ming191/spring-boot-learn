package vn.amela.authservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.amela.authservice.entity.enums.Role;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_ERROR_ATTRIBUTE = "auth_error";
    private static final String INVALID_TOKEN = "Invalid token";
    private static final String MISSING_CLAIMS = "Required JWT claims are missing";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        try {
            if (token.isBlank() || !jwtService.isTokenValid(token)) {
                rejectToken(request, INVALID_TOKEN);
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtService.extractId(token);
            String username = jwtService.extractUsername(token);
            Role role = jwtService.extractRole(token);

            if (username == null || username.isBlank()
                || role == null) {

                rejectToken(request, MISSING_CLAIMS);
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
            );

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                    userId,
                    username,
                    role
                );

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authorities
                    );

                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {
            rejectToken(request, e.getMessage() == null ? INVALID_TOKEN : e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejectToken(HttpServletRequest request, String message) {
        SecurityContextHolder.clearContext();
        request.setAttribute(AUTH_ERROR_ATTRIBUTE, message);
    }
}
