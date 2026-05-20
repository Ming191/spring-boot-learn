package vn.amela.webservice.dto.response;

import lombok.Builder;
import vn.amela.webservice.entity.enums.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EmployeeResponse(
    Long id,
    String employeeCode,
    String fullName,
    String email,
    String phone,
    String position,
    EmployeeStatus status,
    BigDecimal salary,
    Long authUserId,
    Long departmentId,
    String departmentName,
    LocalDate startDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
