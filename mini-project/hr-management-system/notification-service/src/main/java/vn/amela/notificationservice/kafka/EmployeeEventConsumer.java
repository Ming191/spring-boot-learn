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
public class EmployeeEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = {
            "employee.created",
            "employee.status.changed",
            "employee.deactivated"
    })
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received employee event topic={}, key={}", record.topic(), record.key());
        notificationService.handle(record);
    }
}
