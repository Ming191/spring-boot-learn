package vn.amela.authservice.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("continues filter chain when Authorization header is absent")
    void continuesWithoutBearerToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).isTokenValid("token");
    }

    @Test
    @DisplayName("sets authentication from valid JWT claims")
    void authenticatesValidJwt() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(jwtService.extractId("token")).thenReturn(99L);
        when(jwtService.extractUsername("token")).thenReturn("hr_admin");
        when(jwtService.extractRole("token")).thenReturn("HR");

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("hr_admin");
        assertThat(authentication.getDetails()).isEqualTo(99L);
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_HR");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("marks request and clears context for invalid JWT")
    void rejectsInvalidJwt() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtService.isTokenValid("token")).thenThrow(new JwtException("bad token"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("auth_error")).isEqualTo("bad token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("marks request when required JWT claims are missing")
    void rejectsMissingClaims() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(jwtService.extractId("token")).thenReturn(99L);
        when(jwtService.extractUsername("token")).thenReturn("hr_admin");
        when(jwtService.extractRole("token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("auth_error")).isEqualTo("Required JWT claims are missing");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
