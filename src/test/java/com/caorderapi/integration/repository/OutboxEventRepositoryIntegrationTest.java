package com.caorderapi.integration.repository;

import com.caorderapi.dto.OutboxEntityPayload;
import com.caorderapi.enums.AggregateType;
import com.caorderapi.enums.OutboxEventType;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OutboxEventRepository;
import com.caorderapi.service.IInventoryCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
@Transactional
class OutboxEventRepositoryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private IInventoryCacheService inventoryCacheService;

    @Test
    void findTop200ByProcessedFalseOrderByCreatedAtAsc_returnsOnlyPendingAndOrdered() {
        OutboxEventEntity oldest = saveEvent(false, OutboxEventType.ORDER_CREATED);
        OutboxEventEntity middle = saveEvent(false, OutboxEventType.ORDER_STATUS_CHANGED);
        OutboxEventEntity processed = saveEvent(true, OutboxEventType.STOCK_RELEASE_REQUESTED);

        setCreatedAt(oldest.getId(), Instant.parse("2026-01-01T00:00:00Z"));
        setCreatedAt(middle.getId(), Instant.parse("2026-01-01T00:01:00Z"));
        setCreatedAt(processed.getId(), Instant.parse("2026-01-01T00:02:00Z"));

        var pending = outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc();

        assertThat(pending).hasSize(2);
        assertThat(pending).extracting(OutboxEventEntity::getId)
                .containsExactly(oldest.getId(), middle.getId());
        assertThat(pending).allMatch(event -> !event.isProcessed());
    }

    private OutboxEventEntity saveEvent(boolean processed, OutboxEventType type) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateId(UUID.randomUUID().toString());
        event.setAggregateType(AggregateType.ORDER);
        event.setEventType(type);
        event.setPayload(new OutboxEntityPayload("PENDING", Map.of(UUID.randomUUID().toString(), 2)));
        event.setProcessed(processed);
        event.setRetryCount(0);
        event.setActive(true);
        return outboxEventRepository.save(event);
    }

    private void setCreatedAt(UUID eventId, Instant instant) {
        jdbcTemplate.update(
                "update outbox_events set created_at = ? where outbox_event_id = ?",
                Timestamp.from(instant),
                eventId
        );
    }
}

