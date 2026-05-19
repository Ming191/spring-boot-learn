package vn.amela.employeeservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.time.LocalDate;

@Data
public class EmployeeFilterRequest {
    private String likeName;
    private Long departmentId;
    private String position;
    private EmployeeStatus status;
    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private int page = 0;
    @Min(value = 1, message = "Size must be greater than or equal to 1")
    private int size = 10;
    private String sortBy;
    private String sortDirection;
}
