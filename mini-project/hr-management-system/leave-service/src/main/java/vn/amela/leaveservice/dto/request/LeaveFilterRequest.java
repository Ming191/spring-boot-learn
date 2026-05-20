package vn.amela.leaveservice.dto.request;


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
    int page,
    int size,
    String sortBy,
    String sortDirection
) { }