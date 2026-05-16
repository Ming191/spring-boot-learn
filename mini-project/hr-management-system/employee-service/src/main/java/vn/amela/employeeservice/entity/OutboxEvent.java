package vn.amela.employeeservice.entity;

import lombok.Data;
import vn.amela.employeeservice.entity.enums.OutboxEventStatus;

import java.time.LocalDateTime;


@Data
public class OutboxEvent {

    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private OutboxEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
