package vn.amela.employeeservice.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.OutboxEventStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "verify.mysql", matches = "true")
class OutboxEventMapperMysqlVerificationTests {

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Test
    void verifiesOutboxInsertAndStatusTransitionsAgainstComposeMysql() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(999L)
                .eventType("employee.created")
                .payload("""
                        {"eventType":"employee.created","aggregateType":"Employee","aggregateId":999}
                        """)
                .build();

        outboxEventMapper.insert(event);

        assertThat(event.getId()).isNotNull();

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusMinutes(1);

        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .contains(event.getId());

        assertThat(outboxEventMapper.claimPending(event.getId(), processingTimeoutAt)).isEqualTo(1);

        OutboxEvent pendingEvent = outboxEventMapper.findById(event.getId());

        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(pendingEvent.getProcessingStartedAt()).isNotNull();
        assertThat(pendingEvent.getPublishedAt()).isNull();

        assertThat(outboxEventMapper.markPublished(event.getId())).isEqualTo(1);

        OutboxEvent publishedEvent = outboxEventMapper.findById(event.getId());

        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        assertThat(publishedEvent.getProcessingStartedAt()).isNull();
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .doesNotContain(event.getId());
    }
}
