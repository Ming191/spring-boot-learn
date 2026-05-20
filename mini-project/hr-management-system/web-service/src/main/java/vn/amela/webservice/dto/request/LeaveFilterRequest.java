package vn.amela.webservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import vn.amela.webservice.entity.enums.LeaveStatus;
import vn.amela.webservice.entity.enums.LeaveType;

import java.time.LocalDate;

@Builder
public record LeaveFilterRequest(
    Long employeeId,
    LeaveStatus status,
    LeaveType leaveType,
    LocalDate fromDate,
    LocalDate toDate,
    String departmentName,

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    Integer page,

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    Integer size,

    String sortBy,
    String sortDirection
) {
    public LeaveFilterRequest {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
    }
}
