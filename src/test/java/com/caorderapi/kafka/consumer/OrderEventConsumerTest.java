package com.caorderapi.kafka.consumer;

import com.caorderapi.dto.OutboxEntityPayload;
import com.caorderapi.kafka.model.OrderEvent;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IInventoryCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock private IInventoryCacheService redisInventoryCacheService;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderEventConsumer consumer;

    private OrderEvent event(String eventType, String status) {
        String pid = UUID.randomUUID().toString();
        return new OrderEvent(UUID.randomUUID().toString(), "ORDER", eventType,
                new OutboxEntityPayload(status, Map.of(pid, 3)));
    }

    @Test
    void consume_orderStatusChangedPaid_decrementsDbStock() {
        OrderEvent e = event("ORDER_STATUS_CHANGED", "PAID");
        consumer.consume(e);
        verify(productRepository, times(1))
                .decrementStockIfAvailable(any(UUID.class), eq(3));
        verify(redisInventoryCacheService, never()).releaseStock(any(), anyInt());
    }

    @Test
    void consume_orderStatusChangedCancelled_releasesRedisStock() {
        OrderEvent e = event("ORDER_STATUS_CHANGED", "CANCELLED");
        consumer.consume(e);
        verify(redisInventoryCacheService, times(1)).releaseStock(any(UUID.class), eq(3));
        verify(productRepository, never()).decrementStockIfAvailable(any(), anyInt());
    }

    @Test
    void consume_stockReleaseRequested_releasesRedisStock() {
        OrderEvent e = event("STOCK_RELEASE_REQUESTED", "PENDING");
        consumer.consume(e);
        verify(redisInventoryCacheService, times(1)).releaseStock(any(UUID.class), eq(3));
    }

    @Test
    void consume_unknownEventType_doesNothing() {
        OrderEvent e = event("ORDER_CREATED", "PENDING");
        consumer.consume(e);
        verify(redisInventoryCacheService, never()).releaseStock(any(), anyInt());
        verify(productRepository, never()).decrementStockIfAvailable(any(), anyInt());
    }
}
