package vn.amela.employeeservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.mapper.OutboxEventMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {
    private final OutboxEventMapper outboxEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventMapper.findPending(100);
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), String.valueOf(event.getAggregateId()), event.getPayload()).get();
                outboxEventMapper.markPublished(event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}", event.getId(), e);
                outboxEventMapper.markFailed(event.getId());
            }
        }
    }
}
