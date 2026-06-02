package vn.amela.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.amela.notificationservice.config.NotificationTemplateProperties;
import vn.amela.notificationservice.entity.Notification;
import vn.amela.notificationservice.mapper.NotificationMapper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_]+)}");
    private static final String SYSTEM_RECIPIENT = "SYSTEM";

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final NotificationTemplateProperties templateProperties;

    @Transactional
    public void handle(ConsumerRecord<String, String> record) {
        JsonNode event = parsePayload(record.value());
        String eventType = text(event, "eventType", record.topic());
        Long aggregateId = number(event, "aggregateId");
        String eventId = eventId(record, eventType, aggregateId);

        if (notificationMapper.existsByEventId(eventId)) {
            log.info("Skip duplicated notification eventId={}, topic={}", eventId, record.topic());
            return;
        }

        Notification notification = buildNotification(eventId, eventType, aggregateId, event, record.value());

        try {
            notificationMapper.insert(notification);
            log.info("Saved notification eventId={}, eventType={}, recipientType={}, recipientId={}",
                    eventId,
                    eventType,
                    notification.getRecipientType(),
                    notification.getRecipientId()
            );
        } catch (DuplicateKeyException duplicate) {
            log.info("Skip duplicated notification eventId={}, topic={}", eventId, record.topic());
        }
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Kafka event payload", exception);
        }
    }

    private Notification buildNotification(
            String eventId,
            String eventType,
            Long aggregateId,
            JsonNode event,
            String rawPayload
    ) {
        Notification.NotificationBuilder builder = Notification.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(text(event, "aggregateType", null))
                .aggregateId(aggregateId)
                .payload(rawPayload);

        NotificationTemplateProperties.Template template = templateProperties.template(eventType);
        String recipientType = template == null || template.getRecipientType() == null || template.getRecipientType().isBlank()
                ? SYSTEM_RECIPIENT
                : template.getRecipientType();
        builder
                .recipientType(recipientType)
                .recipientId(template == null ? null : number(event, template.getRecipientIdField()))
                .title(render(template == null ? eventType : template.getTitle(), eventType, aggregateId, event))
                .message(render(template == null ? eventType : template.getMessage(), eventType, aggregateId, event));

        return builder.build();
    }

    private String render(String template, String eventType, Long aggregateId, JsonNode event) {
        if (template == null || template.isBlank()) {
            return eventType;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String field = matcher.group(1);
            String value = switch (field) {
                case "eventType" -> eventType;
                case "aggregateId" -> Objects.toString(aggregateId, "");
                default -> text(event, field, "");
            };
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String eventId(ConsumerRecord<String, String> record, String eventType, Long aggregateId) {
        String headerEventId = header(record, "eventId");
        if (headerEventId != null && !headerEventId.isBlank()) {
            return headerEventId;
        }

        String headerOutboxId = header(record, "outboxId");
        if (headerOutboxId != null && !headerOutboxId.isBlank()) {
            return headerOutboxId;
        }

        return eventType + ":" + Objects.toString(aggregateId, record.key());
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asString(defaultValue);
    }

    private Long number(JsonNode node, String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        String text = value.asString();
        if (text == null || text.isBlank()) {
            return null;
        }
        return Long.valueOf(text);
    }
}
