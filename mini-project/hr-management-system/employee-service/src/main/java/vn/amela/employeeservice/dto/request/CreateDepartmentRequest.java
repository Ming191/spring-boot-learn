package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record CreateDepartmentRequest(
        @NotBlank(message = "Name is required")
        String name,
        @Length(max = 255)
        String description,
        Long managerId
) {}
