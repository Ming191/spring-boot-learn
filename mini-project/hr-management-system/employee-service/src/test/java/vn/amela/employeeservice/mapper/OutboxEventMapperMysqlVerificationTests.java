package vn.amela.employeeservice.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.OutboxEventStatus;

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

        OutboxEvent pendingEvent = outboxEventMapper.findPending(10)
                .stream()
                .filter(found -> found.getId().equals(event.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(pendingEvent.getPublishedAt()).isNull();

        assertThat(outboxEventMapper.markPublished(event.getId())).isEqualTo(1);

        OutboxEvent publishedEvent = outboxEventMapper.findPending(10)
                .stream()
                .filter(found -> found.getId().equals(event.getId()))
                .findFirst()
                .orElse(null);

        assertThat(publishedEvent).isNull();
    }
}
