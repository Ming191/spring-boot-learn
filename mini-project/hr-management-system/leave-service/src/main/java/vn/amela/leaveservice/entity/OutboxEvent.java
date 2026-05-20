package vn.amela.leaveservice.entity;


import lombok.Builder;
import lombok.Data;
import vn.amela.leaveservice.entity.enums.OutboxEventStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class OutboxEvent {
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private OutboxEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
