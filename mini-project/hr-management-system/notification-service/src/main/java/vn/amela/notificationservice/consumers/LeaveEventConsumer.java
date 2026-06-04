package vn.amela.notificationservice.consumers;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.amela.notificationservice.service.NotificationEventService;

@Component
@RequiredArgsConstructor
public class LeaveEventConsumer {

    private final NotificationEventService notificationEventService;

    @KafkaListener(
            topics = {"leave.requested", "leave.approved", "leave.rejected", "leave.cancelled"},
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        notificationEventService.handle(record);
    }
}
