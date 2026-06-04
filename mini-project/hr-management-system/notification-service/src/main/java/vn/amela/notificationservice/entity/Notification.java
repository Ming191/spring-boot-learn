package vn.amela.notificationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private String recipientRole;
    private Long recipientEmployeeId;
    private String message;
    private String payload;
    private LocalDateTime createdAt;
}
