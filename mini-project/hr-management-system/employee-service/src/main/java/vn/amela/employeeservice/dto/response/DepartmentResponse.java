package vn.amela.employeeservice.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DepartmentResponse(
        Long id,
        String name,
        String description,
        Long managerId,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}