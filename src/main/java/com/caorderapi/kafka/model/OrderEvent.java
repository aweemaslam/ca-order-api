package com.caorderapi.kafka.model;

/**
 * Kafka payload for order-related events. Converted to a Java record for
 * immutability and simple JSON (de)serialization.
 */
public record OrderEvent(
        String aggregateId,
        String aggregateType,
        String eventType,
        String payload
) {
}