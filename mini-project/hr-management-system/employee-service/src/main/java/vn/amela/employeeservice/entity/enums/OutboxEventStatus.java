package vn.amela.employeeservice.entity.enums;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
