package com.caorderapi.service.listener;

import com.caorderapi.event.OutboxDispatchRequestedEvent;
import com.caorderapi.service.scheduler.OutboxPublisherJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to committed outbox writes and triggers asynchronous dispatch immediately.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatchListener {

    private final OutboxPublisherJob outboxPublisherJob;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxDispatchRequested(OutboxDispatchRequestedEvent event) {
        log.debug("Received outbox dispatch signal; starting async publish cycle");
        outboxPublisherJob.publish();
    }
}

