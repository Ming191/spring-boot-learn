package vn.amela.employeeservice.dto.request;

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
    private int page = 0;
    private int size = 10;
    private String sortBy;
    private String sortDirection;
}
