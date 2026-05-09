package vn.amela.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.amela.authservice.entity.enums.Role;

@Data
public class RegisterRequest {
    @NotBlank( message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank( message = "Password is required")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

    @NotBlank( message = "Email is required")
    @Email( message = "Email is invalid")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @NotBlank( message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    private Role role = Role.EMPLOYEE;
}
