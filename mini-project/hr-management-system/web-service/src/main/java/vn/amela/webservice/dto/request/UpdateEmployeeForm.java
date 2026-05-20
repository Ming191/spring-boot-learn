package vn.amela.webservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record UpdateEmployeeForm(
    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    String email,

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
    String phone,

    @NotBlank(message = "Position is required")
    String position,

    @NotNull(message = "Department id is required")
    Long departmentId,

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be positive")
    BigDecimal salary,

    @NotNull(message = "Start date is required")
    LocalDate startDate
) {}
