package vn.amela.webservice.dto.response;

import lombok.Builder;
import vn.amela.webservice.entity.enums.LeaveStatus;
import vn.amela.webservice.entity.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record LeaveResponse(
    Long id,
    Long employeeId,
    String employeeCode,
    String employeeName,
    String departmentName,
    LeaveType leaveType,
    LocalDate fromDate,
    LocalDate toDate,
    int totalDays,
    String reason,
    LeaveStatus status,
    Long reviewedBy,
    String reviewerNote,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
