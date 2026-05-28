package com.caorderapi.service.scheduler;

import com.caorderapi.kafka.model.OrderEvent;
import com.caorderapi.kafka.producer.OrderEventProducer;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer producer;

    @Scheduled(fixedDelayString = "${app.jobs.outbox-publish-delay-ms:5000}")
    @SchedulerLock(name = "outboxPublisherJob", lockAtLeastFor = "PT2S", lockAtMostFor = "PT20S")
    @Transactional
    public void publish() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pending) {
            try {
                log.info("Publishing outbox event type={} aggregateId={} payload={}",
                        event.getEventType(), event.getAggregateId(), event.getPayload());
                producer.publish(new OrderEvent(
                        event.getAggregateId(),
                        event.getAggregateType().name(),
                        event.getEventType().name(),
                        event.getPayload()
                ));
                event.setProcessed(true);
                event.setLastError(null);
            } catch (Exception ex) {
                int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
                event.setRetryCount(retries + 1);
                event.setLastError(ex.getMessage());
                if (event.getRetryCount() >= MAX_RETRIES) {
                    // Stop retrying forever after max attempts; keep failure reason for triage.
                    event.setProcessed(true);
                }
                log.error("Outbox publish failed eventId={} retry={}", event.getId(), event.getRetryCount(), ex);
            }
        }
    }
}

