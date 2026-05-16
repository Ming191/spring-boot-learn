package vn.amela.employeeservice.dto.response;

import lombok.Data;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private EmployeeStatus status;
    private BigDecimal salary;
    private Long authUserId;
    private Long departmentId;
    private LocalDate startDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
