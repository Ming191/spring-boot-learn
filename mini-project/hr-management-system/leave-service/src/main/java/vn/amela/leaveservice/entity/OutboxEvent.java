package vn.amela.leaveservice.entity;


import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.amela.leaveservice.entity.enums.OutboxEventStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private OutboxEventStatus status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime processingStartedAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
