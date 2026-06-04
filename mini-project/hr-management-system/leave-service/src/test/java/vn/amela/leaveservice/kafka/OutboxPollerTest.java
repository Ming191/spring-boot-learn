package vn.amela.leaveservice.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import vn.amela.leaveservice.entity.OutboxEvent;
import vn.amela.leaveservice.mapper.OutboxEventMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPollerTest {

    private final OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxPoller outboxPoller = new OutboxPoller(outboxEventMapper, kafkaTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxPoller, "maxRetries", 3);
        ReflectionTestUtils.setField(outboxPoller, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(outboxPoller, "processingTimeoutSeconds", 300L);
        ReflectionTestUtils.setField(outboxPoller, "publishTimeoutMs", 1000L);
    }

    @Test
    void pollAndPublishClaimsByIdSendsRecordWithDeduplicationHeadersAndMarksPublished() {
        OutboxEvent event = outboxEvent(10L, 0);
        when(outboxEventMapper.findPendingIds(eq(100), any(LocalDateTime.class))).thenReturn(List.of(10L));
        when(outboxEventMapper.claimPending(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(outboxEventMapper.findById(10L)).thenReturn(event);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
        when(outboxEventMapper.markPublished(10L)).thenReturn(1);

        outboxPoller.pollAndPublish();

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(outboxEventMapper).claimPending(eq(10L), any(LocalDateTime.class));
        verify(kafkaTemplate).send(recordCaptor.capture());
        verify(outboxEventMapper).markPublished(10L);

        ProducerRecord<String, String> record = recordCaptor.getValue();
        assertThat(record.topic()).isEqualTo("leave.requested");
        assertThat(record.key()).isEqualTo("1");
        assertThat(record.value()).isEqualTo("{\"eventType\":\"leave.requested\"}");
        assertThat(new String(record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8)).isEqualTo("10");
        assertThat(new String(record.headers().lastHeader("outboxId").value(), StandardCharsets.UTF_8)).isEqualTo("10");
    }

    @Test
    void pollAndPublishSchedulesRetryWhenKafkaSendFailsBeforeMaxRetries() {
        OutboxEvent event = outboxEvent(10L, 1);
        when(outboxEventMapper.findPendingIds(eq(100), any(LocalDateTime.class))).thenReturn(List.of(10L));
        when(outboxEventMapper.claimPending(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(outboxEventMapper.findById(10L)).thenReturn(event);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.<SendResult<String, String>>failedFuture(new RuntimeException("kafka down")));

        outboxPoller.pollAndPublish();

        verify(outboxEventMapper).markRetry(eq(10L), any(String.class), any(LocalDateTime.class));
        verify(outboxEventMapper, never()).markPublished(10L);
        verify(outboxEventMapper, never()).markFailed(eq(10L), any(String.class));
    }

    @Test
    void pollAndPublishMarksFailedWhenKafkaSendFailsAtMaxRetries() {
        OutboxEvent event = outboxEvent(10L, 2);
        when(outboxEventMapper.findPendingIds(eq(100), any(LocalDateTime.class))).thenReturn(List.of(10L));
        when(outboxEventMapper.claimPending(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(outboxEventMapper.findById(10L)).thenReturn(event);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.<SendResult<String, String>>failedFuture(new RuntimeException("kafka down")));

        outboxPoller.pollAndPublish();

        verify(outboxEventMapper).markFailed(eq(10L), any(String.class));
        verify(outboxEventMapper, never()).markRetry(eq(10L), any(String.class), any(LocalDateTime.class));
        verify(outboxEventMapper, never()).markPublished(10L);
    }

    private OutboxEvent outboxEvent(Long id, Integer retryCount) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateId(1L)
                .eventType("leave.requested")
                .payload("{\"eventType\":\"leave.requested\"}")
                .retryCount(retryCount)
                .build();
    }
}
