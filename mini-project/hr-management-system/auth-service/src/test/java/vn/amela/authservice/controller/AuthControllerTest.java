package vn.amela.authservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.exception.DuplicateResourceException;
import vn.amela.authservice.security.JwtService;
import vn.amela.authservice.service.AuthService;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("register returns standardized validation error response")
    void registerReturnsValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "x",
                      "password": "short",
                      "email": "not-an-email",
                      "fullName": "A"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Request is invalid"))
            .andExpect(jsonPath("$.path").value("/api/auth/register"))
            .andExpect(jsonPath("$.errors", hasSize(4)))
            .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
            .andExpect(jsonPath("$.errors[*].field", hasItem("fullName")))
            .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
            .andExpect(jsonPath("$.errors[*].field", hasItem("username")));
    }

    @Test
    @DisplayName("register returns standardized duplicate error response")
    void registerReturnsDuplicateErrorResponse() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "emp_test",
                      "password": "password123",
                      "email": "emp_test@company.com",
                      "fullName": "Employee Test"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
            .andExpect(jsonPath("$.message").value("Username already exists"))
            .andExpect(jsonPath("$.path").value("/api/auth/register"))
            .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    @DisplayName("register ignores client role and returns service result")
    void registerIgnoresClientRole() throws Exception {
        when(authService.register(any())).thenReturn(UserResponse.builder()
            .id(1L)
            .username("emp_test")
            .email("emp_test@company.com")
            .fullName("Employee Test")
            .role(Role.EMPLOYEE)
            .isActive(true)
            .build());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "emp_test",
                      "password": "password123",
                      "email": "emp_test@company.com",
                      "fullName": "Employee Test",
                      "role": "HR"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }
}
