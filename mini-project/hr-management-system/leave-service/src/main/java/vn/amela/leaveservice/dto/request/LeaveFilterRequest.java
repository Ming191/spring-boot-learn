package vn.amela.leaveservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;

import java.time.LocalDate;

@Builder
public record LeaveFilterRequest(
        Long employeeId,
        LeaveStatus status,
        LeaveType leaveType,
        LocalDate fromDate,
        LocalDate toDate,
        String departmentName,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sortBy,
        String sortDirection
) {
    public LeaveFilterRequest {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
