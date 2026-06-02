package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.time.LocalDate;

@Builder
public record EmployeeFilterRequest (
        String likeName,
        Long departmentId,
        String position,
        EmployeeStatus status,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        @Min(value = 0, message = "Page must be greater than or equal to 0")
        int page,
        @Min(value = 1, message = "Size must be greater than or equal to 1")
        int size,
        String sortBy,
        String sortDirection
) {
}
