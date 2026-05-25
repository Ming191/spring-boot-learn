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
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
    public EmployeeFilterRequest {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }
    }
}
