package vn.amela.webservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateDepartmentForm(
    @NotBlank(message = "Name is required")
    String name,

    @Size(max = 255, message = "Description must be less than or equal to 255 characters")
    String description,

    Long managerId
) {}
