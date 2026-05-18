package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Builder
public record UpdateDepartmentRequest(
        @NotBlank(message = "Name is required")
        String name,
        @Length(max = 255)
        String description,
        Long managerId,
        Boolean isActive
) {}