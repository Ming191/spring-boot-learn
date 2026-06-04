package vn.amela.leaveservice.kafka;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.amela.leaveservice.entity.OutboxEvent;
import vn.amela.leaveservice.mapper.OutboxEventMapper;

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

    @Value("${app.outbox.processing-timeout-seconds:300}")
    private long processingTimeoutSeconds;

    @Value("${app.outbox.publish-timeout-ms:10000}")
    private long publishTimeoutMs;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:5000}")
    public void pollAndPublish() {
        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        List<Long> pendingEventIds = outboxEventMapper.findPendingIds(BATCH_SIZE, processingTimeoutAt);
        for (Long eventId : pendingEventIds) {
            if (outboxEventMapper.claimPending(eventId, processingTimeoutAt) == 0) {
                continue;
            }

            OutboxEvent event = outboxEventMapper.findById(eventId);
            if (event == null) {
                LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(retryDelaySeconds);
                outboxEventMapper.markRetry(eventId, "Claimed outbox event could not be loaded", nextRetryAt);
                log.warn("Claimed outbox event id={} could not be loaded and was released for retry", eventId);
                continue;
            }

            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        String topic = event.getEventType();
        String key = String.valueOf(event.getAggregateId());
        String payload = event.getPayload();

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            addDeduplicationHeaders(record, event);
            kafkaTemplate.send(record).get(publishTimeoutMs, TimeUnit.MILLISECONDS);

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
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error(
                    "Interrupted while publishing outbox event id={}, topic={}, aggregateId={}",
                    event.getId(),
                    topic,
                    event.getAggregateId(),
                    ex
            );
            handlePublishFailure(event, ex);
        } catch (ExecutionException | TimeoutException ex) {
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

        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(retryDelaySeconds);
        outboxEventMapper.markRetry(event.getId(), lastError, nextRetryAt);
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

    private void addDeduplicationHeaders(ProducerRecord<String, String> record, OutboxEvent event) {
        byte[] eventId = String.valueOf(event.getId()).getBytes(StandardCharsets.UTF_8);
        record.headers().add(new RecordHeader("eventId", eventId));
        record.headers().add(new RecordHeader("outboxId", eventId));
    }
}
