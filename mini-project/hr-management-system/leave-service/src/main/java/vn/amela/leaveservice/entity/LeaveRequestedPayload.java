package vn.amela.leaveservice.entity;

import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveRequestedPayload(
        String eventType,
        String aggregateType,
        Long aggregateId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        LeaveType leaveType,
        LocalDate fromDate,
        LocalDate toDate,
        int totalDays,
        LeaveStatus status,
        Instant timestamp
) {
}
