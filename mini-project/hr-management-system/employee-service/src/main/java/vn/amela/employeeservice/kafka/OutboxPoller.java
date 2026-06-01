package vn.amela.employeeservice.kafka;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.mapper.OutboxEventMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final OutboxEventMapper outboxEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${app.outbox.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:5000}")
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventMapper.findPending(BATCH_SIZE);
        for (OutboxEvent event : pendingEvents) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        String topic = event.getEventType();
        String key = String.valueOf(event.getAggregateId());
        String payload = event.getPayload();

        try {
            kafkaTemplate.send(topic, key, payload).get();

            int updatedRows = outboxEventMapper.markPublished(event.getId());
            if (updatedRows == 0) {
                log.warn("Outbox event id={} could not be marked as published", event.getId());
                return;
            }

            log.info(
                    "Published outbox event id={}, topic={}, aggregateId={}",
                    event.getId(),
                    topic,
                    event.getAggregateId()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to publish outbox event id={}, topic={}, aggregateId={}",
                    event.getId(),
                    topic,
                    event.getAggregateId(),
                    ex
            );
            handlePublishFailure(event, ex);
        }
    }

    private void handlePublishFailure(OutboxEvent event, Exception ex) {
        String lastError = truncateError(ex.getMessage());
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();
        int nextAttempt = retryCount + 1;

        if (nextAttempt >= maxRetries) {
            outboxEventMapper.markFailed(event.getId(), lastError);
            log.warn("Outbox event id={} marked FAILED after {} attempts", event.getId(), nextAttempt);
            return;
        }

        outboxEventMapper.markRetry(event.getId(), lastError, retryDelaySeconds);
        log.info(
                "Outbox event id={} scheduled for retry attempt={} after {}s",
                event.getId(),
                nextAttempt,
                retryDelaySeconds
        );
    }

    private String truncateError(String message) {
        if (message == null || message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }
}
