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

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusDays(1);

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

    @Test
    void duplicateClaimReturnsZeroWhenProcessingIsNotStale() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(1000L)
                .eventType("employee.created")
                .payload("""
                        {"eventType":"employee.created","aggregateType":"Employee","aggregateId":1000}
                        """)
                .build();

        outboxEventMapper.insert(event);

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusDays(1);

        assertThat(outboxEventMapper.claimPending(event.getId(), processingTimeoutAt)).isEqualTo(1);
        assertThat(outboxEventMapper.claimPending(event.getId(), processingTimeoutAt)).isZero();

        OutboxEvent claimedEvent = outboxEventMapper.findById(event.getId());

        assertThat(claimedEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(claimedEvent.getProcessingStartedAt()).isNotNull();
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .doesNotContain(event.getId());
    }

    @Test
    void markRetryReleasesProcessingEventAndRespectsRetryDueLogic() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(1001L)
                .eventType("employee.created")
                .payload("""
                        {"eventType":"employee.created","aggregateType":"Employee","aggregateId":1001}
                        """)
                .build();
        OutboxEvent dueEvent = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(1002L)
                .eventType("employee.created")
                .payload("""
                        {"eventType":"employee.created","aggregateType":"Employee","aggregateId":1002}
                        """)
                .build();

        outboxEventMapper.insert(event);
        outboxEventMapper.insert(dueEvent);

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusDays(1);
        LocalDateTime nextRetryAt = LocalDateTime.now().plusDays(1);
        LocalDateTime dueNextRetryAt = LocalDateTime.now().minusDays(1);
        String lastError = "Kafka publish failed";

        assertThat(outboxEventMapper.claimPending(event.getId(), processingTimeoutAt)).isEqualTo(1);
        assertThat(outboxEventMapper.claimPending(dueEvent.getId(), processingTimeoutAt)).isEqualTo(1);
        assertThat(outboxEventMapper.markRetry(event.getId(), lastError, nextRetryAt)).isEqualTo(1);
        assertThat(outboxEventMapper.markRetry(dueEvent.getId(), lastError, dueNextRetryAt)).isEqualTo(1);

        OutboxEvent retryEvent = outboxEventMapper.findById(event.getId());
        OutboxEvent dueRetryEvent = outboxEventMapper.findById(dueEvent.getId());

        assertThat(retryEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(retryEvent.getProcessingStartedAt()).isNull();
        assertThat(retryEvent.getRetryCount()).isEqualTo(1);
        assertThat(retryEvent.getLastError()).isEqualTo(lastError);
        assertThat(retryEvent.getNextRetryAt()).isNotNull();
        assertThat(retryEvent.getNextRetryAt()).isAfterOrEqualTo(nextRetryAt.minusSeconds(1));
        assertThat(retryEvent.getNextRetryAt()).isBeforeOrEqualTo(nextRetryAt.plusSeconds(1));
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .doesNotContain(event.getId());

        assertThat(dueRetryEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(dueRetryEvent.getProcessingStartedAt()).isNull();
        assertThat(dueRetryEvent.getRetryCount()).isEqualTo(1);
        assertThat(dueRetryEvent.getLastError()).isEqualTo(lastError);
        assertThat(dueRetryEvent.getNextRetryAt()).isNotNull();
        assertThat(dueRetryEvent.getNextRetryAt()).isAfterOrEqualTo(dueNextRetryAt.minusSeconds(1));
        assertThat(dueRetryEvent.getNextRetryAt()).isBeforeOrEqualTo(dueNextRetryAt.plusSeconds(1));
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .contains(dueEvent.getId());
    }
}
