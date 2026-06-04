package vn.amela.authservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
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
import vn.amela.authservice.controller.AuthViewController;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.security.handler.CustomAccessDeniedHandler;
import vn.amela.authservice.security.handler.CustomAuthenticationEntryPoint;
import vn.amela.authservice.security.handler.SecurityErrorResponseWriter;
import vn.amela.authservice.service.AuthService;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    AuthController.class,
    AuthViewController.class
})
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
        when(jwtService.extractRole("employee-token")).thenReturn(Role.EMPLOYEE);

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

    @Test
    @DisplayName("me returns typed current user response")
    void meReturnsTypedCurrentUserResponse() throws Exception {
        when(jwtService.isTokenValid("hr-token")).thenReturn(true);
        when(jwtService.extractId("hr-token")).thenReturn(7L);
        when(jwtService.extractUsername("hr-token")).thenReturn("hr_admin");
        when(jwtService.extractRole("hr-token")).thenReturn(Role.HR);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer hr-token"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(7))
            .andExpect(jsonPath("$.username").value("hr_admin"))
            .andExpect(jsonPath("$.role").value("HR"))
            .andExpect(jsonPath("$.authorities").doesNotExist());
    }

    @Test
    @DisplayName("protected endpoint accepts access token from HttpOnly UI cookie")
    void protectedEndpointAcceptsCookieToken() throws Exception {
        when(jwtService.isTokenValid("cookie-token")).thenReturn(true);
        when(jwtService.extractId("cookie-token")).thenReturn(8L);
        when(jwtService.extractUsername("cookie-token")).thenReturn("emp_cookie");
        when(jwtService.extractRole("cookie-token")).thenReturn(Role.EMPLOYEE);

        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("HR_ACCESS_TOKEN", "cookie-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(8))
            .andExpect(jsonPath("$.username").value("emp_cookie"))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("logout endpoint is public at auth service for tokenless clients")
    void logoutWithoutTokenReturnsRedirect() throws Exception {
        mockMvc.perform(post("/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }
}
