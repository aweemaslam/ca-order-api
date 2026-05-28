package com.caorderapi.kafka.consumer;

import com.caorderapi.kafka.model.OrderEvent;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IInventoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final IInventoryCacheService redisInventoryCacheService;
    private final ProductRepository productRepository;

    @KafkaListener(topics = "${spring.kafka.topic.order-event:order-events}",
            groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consume(OrderEvent event) {

        log.info("Received event: {}", event);

        switch (event.eventType()) {
            case "ORDER_STATUS_CHANGED" -> handleOrderStatusChanged(event);
            case "STOCK_RELEASE_REQUESTED" -> handleStockRelease(event);
        }
    }

    private void handleStockRelease(OrderEvent event) {
        event.payload().productsWithQuantity().forEach((key, value) -> {
            redisInventoryCacheService.releaseStock(UUID.fromString(key), value);
            //productRepository.incrementStock(UUID.fromString(key), value);
        });
    }

    private void handleOrderStatusChanged(OrderEvent event) {
        switch (event.payload().status()) {
            // update product stock in db once payment is confirmed
            case "PAID" -> event.payload().productsWithQuantity().forEach((key, value) -> {
                productRepository.decrementStockIfAvailable(UUID.fromString(key), value);
            });
            // revert the stock in redis cache
            case "CANCELLED" -> handleStockRelease(event);

        }
    }
}