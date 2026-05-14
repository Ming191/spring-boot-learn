package vn.amela.authservice.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.amela.authservice.config.SecurityConfig;
import vn.amela.authservice.controller.AuthController;
import vn.amela.authservice.security.handler.CustomAccessDeniedHandler;
import vn.amela.authservice.security.handler.CustomAuthenticationEntryPoint;
import vn.amela.authservice.security.handler.SecurityErrorResponseWriter;
import vn.amela.authservice.service.AuthService;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("protected endpoint without token returns JSON 401")
    void protectedEndpointWithoutTokenReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Authentication is required"))
            .andExpect(jsonPath("$.path").value("/api/auth/me"))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    @DisplayName("protected endpoint with invalid token returns JSON 401")
    void protectedEndpointWithInvalidTokenReturnsJson401() throws Exception {
        when(jwtService.isTokenValid("bad-token")).thenThrow(new JwtException("bad token"));

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer bad-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Authentication is required"))
            .andExpect(jsonPath("$.path").value("/api/auth/me"))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    @DisplayName("admin endpoint with employee role returns JSON 403")
    void adminEndpointWithEmployeeRoleReturnsJson403() throws Exception {
        when(jwtService.isTokenValid("employee-token")).thenReturn(true);
        when(jwtService.extractId("employee-token")).thenReturn(1L);
        when(jwtService.extractUsername("employee-token")).thenReturn("emp");
        when(jwtService.extractRole("employee-token")).thenReturn("EMPLOYEE");

        mockMvc.perform(get("/api/auth/admin/users")
                .header("Authorization", "Bearer employee-token"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
            .andExpect(jsonPath("$.path").value("/api/auth/admin/users"))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }
}
