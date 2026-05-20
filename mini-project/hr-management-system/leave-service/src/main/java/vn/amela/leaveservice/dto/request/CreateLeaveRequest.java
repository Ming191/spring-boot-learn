package vn.amela.leaveservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import vn.amela.leaveservice.entity.enums.LeaveType;

import java.time.LocalDate;

@Builder
public record CreateLeaveRequest(
        @NotNull(message = "Leave type cannot be null")
        LeaveType leaveType,
        @NotNull(message = "From date cannot be null")
        LocalDate fromDate,
        @NotNull(message = "To date cannot be null")
        LocalDate toDate,
        @NotBlank(message = "Reason cannot be blank")
        String reason
) { }
