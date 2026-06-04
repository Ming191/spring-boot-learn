package vn.amela.leaveservice.entity;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LeaveApprovedPayload(
        String eventType,
        String aggregateType,
        Long aggregateId,
        Long employeeId,
        String employeeName,
        String reviewerNote,
        Instant timestamp
) {
}
