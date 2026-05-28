package com.caorderapi.kafka.consumer;

import com.caorderapi.kafka.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void consume(OrderEvent event) {

        log.info("Received event: {}", event);

        switch (event.eventType()) {
            case "ORDER_CREATED" -> handleCreated(event);
            case "ORDER_STATUS_CHANGED" -> handlePaid(event);
            case "STOCK_RELEASE_REQUESTED" -> handleStockRelease(event);
        }
    }

    private void handleStockRelease(OrderEvent event) {
    }

    private void handleCreated(OrderEvent event) {
        // call payment service or saga start
    }

    private void handlePaid(OrderEvent event) {
        // trigger fulfillment
    }
}