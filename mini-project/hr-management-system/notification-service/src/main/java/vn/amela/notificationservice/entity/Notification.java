package vn.amela.notificationservice.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Notification {
    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private String recipientType;
    private Long recipientId;
    private String title;
    private String message;
    private String payload;
    private Boolean read;
    private LocalDateTime createdAt;
}
