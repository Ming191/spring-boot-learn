package vn.amela.employeeservice.entity.payload;

import java.time.Instant;

public record EmployeeUpdatePayload(
        String eventType,
        String aggregateType,
        Long aggregateId,
        String fullName,
        String email,
        Long departmentId,
        String position,
        Instant timestamp
) {
}
