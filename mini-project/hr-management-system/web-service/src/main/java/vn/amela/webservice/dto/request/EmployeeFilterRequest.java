package vn.amela.webservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import vn.amela.webservice.entity.enums.EmployeeStatus;

import java.time.LocalDate;

@Builder
public record EmployeeFilterRequest(
    String likeName,
    Long departmentId,
    String position,
    EmployeeStatus status,
    LocalDate startDateFrom,
    LocalDate startDateTo,

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    Integer page,

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    Integer size,

    String sortBy,
    String sortDirection
) {
    public EmployeeFilterRequest {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
    }
}
