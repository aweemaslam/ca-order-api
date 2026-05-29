package com.caorderapi.service.impl;

import com.caorderapi.enums.AggregateType;
import com.caorderapi.enums.OutboxEventType;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OutboxEventRepository;
import com.caorderapi.util.OrderTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @InjectMocks private OutboxEventService service;

    @Test
    void saveOrderCreatedOutbox_savesWithCorrectEventType() {
        var order = OrderTestFactory.pendingOrder();
        service.saveOrderCreatedOutbox(order);

        ArgumentCaptor<OutboxEventEntity> cap = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(cap.capture());
        OutboxEventEntity saved = cap.getValue();

        assertThat(saved.getEventType()).isEqualTo(OutboxEventType.ORDER_CREATED);
        assertThat(saved.getAggregateType()).isEqualTo(AggregateType.ORDER);
        assertThat(saved.getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getRetryCount()).isEqualTo(0);
    }

    @Test
    void saveStatusChangedOutbox_savesWithCorrectEventType() {
        var order = OrderTestFactory.pendingOrder();
        service.saveStatusChangedOutbox(order);

        ArgumentCaptor<OutboxEventEntity> cap = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo(OutboxEventType.ORDER_STATUS_CHANGED);
    }

    @Test
    void saveStockReleaseOutbox_savesWithCorrectEventType() {
        var order = OrderTestFactory.pendingOrder();
        service.saveStockReleaseOutbox(order);

        ArgumentCaptor<OutboxEventEntity> cap = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo(OutboxEventType.STOCK_RELEASE_REQUESTED);
    }

    @Test
    void saveOrderCreatedOutbox_multipleItemsSameProduct_sumsQuantities() {
        var order = OrderTestFactory.pendingOrder();
        // Add second item with same product
        var item2 = OrderTestFactory.orderItem(order);
        item2.setQuantity(3);
        order.getItems().add(item2);

        service.saveOrderCreatedOutbox(order);

        ArgumentCaptor<OutboxEventEntity> cap = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(cap.capture());
        var qty = cap.getValue().getPayload().productsWithQuantity()
                .get(OrderTestFactory.PRODUCT_ID.toString());
        assertThat(qty).isEqualTo(5); // 2 + 3
    }

    @Test
    void saveOrderCreatedOutbox_payloadStatusMatchesOrderStatus() {
        var order = OrderTestFactory.pendingOrder();
        service.saveOrderCreatedOutbox(order);

        ArgumentCaptor<OutboxEventEntity> cap = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(cap.capture());
        assertThat(cap.getValue().getPayload().status()).isEqualTo("PENDING");
    }
}
