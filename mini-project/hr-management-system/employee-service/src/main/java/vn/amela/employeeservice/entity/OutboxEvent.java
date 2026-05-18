package vn.amela.employeeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.amela.employeeservice.entity.enums.OutboxEventStatus;

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
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

}
