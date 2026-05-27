package com.caorderapi.event;

/**
 * Lightweight signal indicating that new outbox rows are ready for asynchronous dispatch.
 */
public record OutboxDispatchRequestedEvent() {
}

