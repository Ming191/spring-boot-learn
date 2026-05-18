package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank(message = "Employee code is required")
        String employeeCode,
        @NotBlank(message = "Full name is required")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        @NotBlank(message = "Phone is required")
        String phone,
        @NotBlank(message = "Position is required")
        String position,
        @NotNull(message = "Department id is required")
        Long departmentId,
        @NotNull(message = "Auth user id is required")
        Long authUserId,
        @NotNull(message = "Salary is required")
        BigDecimal salary,
        @NotNull(message = "Start date is required")
        LocalDate startDate
) {}
