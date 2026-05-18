package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateContactRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        @NotBlank(message = "Phone is required")
        String phone
) {}
