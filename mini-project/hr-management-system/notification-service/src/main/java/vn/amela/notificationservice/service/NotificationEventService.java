package vn.amela.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.amela.notificationservice.entity.Notification;
import vn.amela.notificationservice.mapper.NotificationMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventService {

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    public void handle(ConsumerRecord<String, String> record) {
        String payload = record.value();
        JsonNode json = readPayload(record.topic(), payload);
        String eventType = text(json, "eventType", record.topic());
        String aggregateType = text(json, "aggregateType", null);
        Long aggregateId = number(json, "aggregateId");
        Long employeeId = number(json, "employeeId");
        String employeeName = text(json, "employeeName", text(json, "fullName", "employee"));

        Notification notification = Notification.builder()
                .eventId(eventId(record))
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .recipientRole(recipientRole(eventType))
                .recipientEmployeeId(recipientEmployeeId(eventType, employeeId))
                .message(message(eventType, employeeName))
                .payload(payload)
                .build();

        notificationMapper.insertIgnore(notification);
        log.info("Notification stored eventType={}, aggregateId={}, message={}",
                notification.getEventType(), notification.getAggregateId(), notification.getMessage());
    }

    private JsonNode readPayload(String topic, String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Kafka payload for topic " + topic, ex);
        }
    }

    private String eventId(ConsumerRecord<String, String> record) {
        Header eventId = record.headers().lastHeader("eventId");
        if (eventId != null) {
            return new String(eventId.value(), StandardCharsets.UTF_8);
        }
        return record.topic() + ":" + record.partition() + ":" + record.offset();
    }

    private String recipientRole(String eventType) {
        if ("leave.requested".equals(eventType)) {
            return "HR";
        }
        if (eventType != null && (eventType.startsWith("employee.") || eventType.startsWith("leave."))) {
            return "EMPLOYEE";
        }
        return null;
    }

    private Long recipientEmployeeId(String eventType, Long employeeId) {
        if (eventType != null && eventType.startsWith("leave.") && !"leave.requested".equals(eventType)) {
            return employeeId;
        }
        return null;
    }

    private String message(String eventType, String employeeName) {
        return switch (eventType) {
            case "employee.created" -> "Employee created: " + employeeName;
            case "employee.status.changed" -> "Employee status changed: " + employeeName;
            case "employee.deactivated" -> "Employee deactivated: " + employeeName;
            case "leave.requested" -> "New leave request from " + employeeName;
            case "leave.approved" -> "Leave request approved for " + employeeName;
            case "leave.rejected" -> "Leave request rejected for " + employeeName;
            case "leave.cancelled" -> "Leave request cancelled by " + employeeName;
            default -> "Event received: " + eventType;
        };
    }

    private String text(JsonNode json, String field, String defaultValue) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? defaultValue : value.asString();
    }

    private Long number(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }
}
