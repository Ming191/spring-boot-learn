package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateEmployeeRequest {
    @NotBlank( message = "Full name is required")
    private String fullName;
    @NotBlank( message = "Email is required")
    @Email( message = "Email is invalid")
    private String email;
    @NotBlank( message = "Phone is required")
    private String phone;
    @NotBlank( message = "Position is required")
    private String position;
    @NotNull( message = "Department id is required")
    private Long departmentId;
    @NotNull( message = "Salary is required")
    private BigDecimal salary;
    @NotNull( message = "Start date is required")
    private LocalDate startDate;
}
