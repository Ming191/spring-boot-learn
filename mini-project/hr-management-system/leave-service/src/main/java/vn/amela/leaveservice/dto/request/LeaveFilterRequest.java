package vn.amela.leaveservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;

import java.time.LocalDate;

public record LeaveFilterRequest(
    Long employeeId,
    LeaveStatus status,
    LeaveType leaveType,
    LocalDate fromDate,
    LocalDate toDate,
    String departmentName,
    @Min(1) int page,
    @Min(1) @Max(100) int size,
    String sortBy,
    String sortDirection
) { }