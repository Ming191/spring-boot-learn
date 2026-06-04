package vn.amela.employeeservice.entity.payload;

import java.math.BigDecimal;
import java.time.Instant;

public record EmployeeUpdatePayload(
        String eventType,
        String aggregateType,
        Long aggregateId,
        String employeeCode,
        String fullName,
        String email,
        Long oldDepartmentId,
        Long newDepartmentId,
        String position,
        BigDecimal oldSalary,
        BigDecimal newSalary,
        String status,
        Instant timestamp
) {
}
