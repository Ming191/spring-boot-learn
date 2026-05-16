package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateContactRequest {
    @NotBlank( message = "Email is required")
    @Email( message = "Email is invalid")
    private String email;
    @NotBlank( message = "Phone is required")
    private String phone;
}
