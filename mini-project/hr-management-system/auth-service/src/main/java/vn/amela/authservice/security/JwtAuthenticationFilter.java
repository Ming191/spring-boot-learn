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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
            if (!token.isBlank() && jwtService.isTokenValid(token)) {
                Long userId = jwtService.extractId(token);
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);
                if (username == null || username.isBlank() || role == null || role.isBlank()) {
                    throw new IllegalArgumentException("Required JWT claims are missing");
                }

                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                        );

                    authentication.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
             }
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
