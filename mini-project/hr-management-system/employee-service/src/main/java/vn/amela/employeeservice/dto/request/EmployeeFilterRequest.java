package vn.amela.employeeservice.dto.request;

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
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
    public EmployeeFilterRequest {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
    }
}
