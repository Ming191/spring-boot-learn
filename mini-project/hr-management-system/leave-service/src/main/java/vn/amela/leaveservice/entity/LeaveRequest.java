package vn.amela.leaveservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private LeaveType leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDays;
    private String reason;
    private LeaveStatus status;
    private Long reviewedBy;
    private String reviewerNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
