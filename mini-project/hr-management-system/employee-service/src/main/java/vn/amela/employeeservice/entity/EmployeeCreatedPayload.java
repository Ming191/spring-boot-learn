package vn.amela.employeeservice.entity;

import java.time.Instant;

public record EmployeeCreatedPayload(
        String eventType,
        String aggregateType,
        Long aggregateId,
        String employeeCode,
        String fullName,
        String email,
        Long departmentId,
        String position,
        Instant timestamp
) {
}
