package vn.amela.employeeservice.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.mapper.OutboxEventMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPollerTest {

    private final OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxPoller outboxPoller = new OutboxPoller(outboxEventMapper, kafkaTemplate);

    @Test
    void pollAndPublishMarksPublishedAfterKafkaAck() {
        OutboxEvent event = outboxEvent(10L);
        when(outboxEventMapper.findPending(100)).thenReturn(List.of(event));
        when(kafkaTemplate.send("employee.created", "1", "{\"eventType\":\"employee.created\"}"))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPoller.pollAndPublish();

        verify(kafkaTemplate).send("employee.created", "1", "{\"eventType\":\"employee.created\"}");
        verify(outboxEventMapper).markPublished(10L);
        verify(outboxEventMapper, never()).markFailed(10L);
    }

    @Test
    void pollAndPublishMarksFailedWhenKafkaSendFails() {
        OutboxEvent event = outboxEvent(10L);
        when(outboxEventMapper.findPending(100)).thenReturn(List.of(event));
        when(kafkaTemplate.send("employee.created", "1", "{\"eventType\":\"employee.created\"}"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        outboxPoller.pollAndPublish();

        verify(outboxEventMapper).markFailed(10L);
        verify(outboxEventMapper, never()).markPublished(10L);
    }

    private OutboxEvent outboxEvent(Long id) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateId(1L)
                .eventType("employee.created")
                .payload("{\"eventType\":\"employee.created\"}")
                .build();
    }
}
