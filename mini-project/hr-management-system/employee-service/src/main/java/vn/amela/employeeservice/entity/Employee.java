package vn.amela.employeeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private Long id;

    private String employeeCode;

    private String fullName;

    private String email;

    private String phone;

    private String position;

    private Long departmentId;

    private Long authUserId;

    private BigDecimal salary;

    private LocalDate startDate;

    private EmployeeStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
