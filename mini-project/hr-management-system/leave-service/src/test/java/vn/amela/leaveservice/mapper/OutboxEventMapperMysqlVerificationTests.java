package vn.amela.leaveservice.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.leaveservice.entity.OutboxEvent;
import vn.amela.leaveservice.entity.enums.OutboxEventStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "verify.mysql", matches = "true")
class OutboxEventMapperMysqlVerificationTests {

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Test
    void verifiesOutboxInsertClaimAndMarkPublishedAgainstComposeMysql() {
        OutboxEvent event = outboxEvent(999L);

        outboxEventMapper.insert(event);

        assertThat(event.getId()).isNotNull();

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusDays(1);

        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .contains(event.getId());

        assertThat(outboxEventMapper.claimPending(event.getId(), processingTimeoutAt)).isEqualTo(1);

        OutboxEvent claimedEvent = outboxEventMapper.findById(event.getId());

        assertThat(claimedEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(claimedEvent.getProcessingStartedAt()).isNotNull();
        assertThat(claimedEvent.getPublishedAt()).isNull();

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
        OutboxEvent event = outboxEvent(1000L);

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
        OutboxEvent futureRetryEvent = outboxEvent(1001L);
        OutboxEvent dueRetryEvent = outboxEvent(1002L);

        outboxEventMapper.insert(futureRetryEvent);
        outboxEventMapper.insert(dueRetryEvent);

        LocalDateTime processingTimeoutAt = LocalDateTime.now().minusDays(1);
        LocalDateTime futureNextRetryAt = LocalDateTime.now().plusDays(1);
        LocalDateTime dueNextRetryAt = LocalDateTime.now().minusDays(1);
        String lastError = "Kafka publish failed";

        assertThat(outboxEventMapper.claimPending(futureRetryEvent.getId(), processingTimeoutAt)).isEqualTo(1);
        assertThat(outboxEventMapper.claimPending(dueRetryEvent.getId(), processingTimeoutAt)).isEqualTo(1);
        assertThat(outboxEventMapper.markRetry(futureRetryEvent.getId(), lastError, futureNextRetryAt)).isEqualTo(1);
        assertThat(outboxEventMapper.markRetry(dueRetryEvent.getId(), lastError, dueNextRetryAt)).isEqualTo(1);

        OutboxEvent futureRetry = outboxEventMapper.findById(futureRetryEvent.getId());
        OutboxEvent dueRetry = outboxEventMapper.findById(dueRetryEvent.getId());

        assertThat(futureRetry.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(futureRetry.getProcessingStartedAt()).isNull();
        assertThat(futureRetry.getRetryCount()).isEqualTo(1);
        assertThat(futureRetry.getLastError()).isEqualTo(lastError);
        assertThat(futureRetry.getNextRetryAt()).isBetween(futureNextRetryAt.minusSeconds(1), futureNextRetryAt.plusSeconds(1));
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .doesNotContain(futureRetryEvent.getId());

        assertThat(dueRetry.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(dueRetry.getProcessingStartedAt()).isNull();
        assertThat(dueRetry.getRetryCount()).isEqualTo(1);
        assertThat(dueRetry.getLastError()).isEqualTo(lastError);
        assertThat(dueRetry.getNextRetryAt()).isBetween(dueNextRetryAt.minusSeconds(1), dueNextRetryAt.plusSeconds(1));
        assertThat(outboxEventMapper.findPendingIds(10, processingTimeoutAt))
                .contains(dueRetryEvent.getId());
    }

    private OutboxEvent outboxEvent(Long aggregateId) {
        return OutboxEvent.builder()
                .aggregateType("LeaveRequest")
                .aggregateId(aggregateId)
                .eventType("leave.requested")
                .payload("""
                        {"eventType":"leave.requested","aggregateType":"LeaveRequest","aggregateId":%d}
                        """.formatted(aggregateId))
                .build();
    }
}
