package vn.amela.authservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.amela.authservice.dto.response.TokenResponse;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.security.JwtService;
import vn.amela.authservice.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("employee login redirects to own leave page")
    void employeeLoginRedirectsToOwnLeavePage() throws Exception {
        when(authService.login(any())).thenReturn(tokens());
        when(jwtService.extractRole("access-token")).thenReturn(Role.EMPLOYEE);

        mockMvc.perform(post("/login")
                .param("usernameOrEmail", "emp")
                .param("password", "password123"))
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/leaves/my"))
            .andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).contains("HR_ACCESS_TOKEN=access-token"))
                .anySatisfy(cookie -> assertThat(cookie).contains("HR_REFRESH_TOKEN=refresh-token")));
    }

    @Test
    @DisplayName("HR login redirects to employee management page")
    void hrLoginRedirectsToEmployeeManagementPage() throws Exception {
        when(authService.login(any())).thenReturn(tokens());
        when(jwtService.extractRole("access-token")).thenReturn(Role.HR);

        mockMvc.perform(post("/login")
                .param("usernameOrEmail", "hr_admin")
                .param("password", "password123"))
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/employees"));
    }

    private static TokenResponse tokens() {
        return new TokenResponse("access-token", "refresh-token", "Bearer", 900L);
    }
}
