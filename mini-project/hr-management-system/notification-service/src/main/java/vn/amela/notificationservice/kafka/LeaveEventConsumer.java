package vn.amela.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.amela.notificationservice.service.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = {
            "leave.requested",
            "leave.approved",
            "leave.rejected",
            "leave.cancelled"
    })
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received leave event topic={}, key={}", record.topic(), record.key());
        notificationService.handle(record);
    }
}
