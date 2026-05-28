package com.caorderapi.service.impl;

import com.caorderapi.dto.OutboxEntityPayload;
import com.caorderapi.enums.AggregateType;
import com.caorderapi.enums.OutboxEventType;
import com.caorderapi.model.OrderItemEntity;
import com.caorderapi.model.Orders;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OutboxEventRepository;
import com.caorderapi.service.IOutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutboxEventService implements IOutboxEventService {
    private final OutboxEventRepository outboxEventRepository;

    private void saveOutbox(AggregateType aggregateType, String aggregateId, OutboxEventType eventType, OutboxEntityPayload payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setProcessed(false);
        event.setRetryCount(0);
        event.setLastError(null);
        outboxEventRepository.save(event);
    }

    @Override
    public void saveStockReleaseOutbox(Orders order) {
        saveOutbox(AggregateType.ORDER, order.getId().toString(), OutboxEventType.STOCK_RELEASE_REQUESTED,
                new OutboxEntityPayload(
                        order.getStatus().getStatusCode(),

                        Optional.ofNullable(order.getItems())
                                .orElse(Collections.emptyList())
                                .stream()
                                .collect(Collectors.toMap(
                                        item -> item.getProductId().toString(),
                                        OrderItemEntity::getQuantity,
                                        Integer::sum
                                ))
                )
        );
    }

    @Override
    public void saveStatusChangedOutbox(Orders order) {
        saveOutbox(AggregateType.ORDER, order.getId().toString(), OutboxEventType.ORDER_STATUS_CHANGED,
                new OutboxEntityPayload(
                        order.getStatus().getStatusCode(),

                        Optional.ofNullable(order.getItems())
                                .orElse(Collections.emptyList())
                                .stream()
                                .collect(Collectors.toMap(
                                        item -> item.getProductId().toString(),
                                        OrderItemEntity::getQuantity,
                                        Integer::sum
                                ))
                )
        );
    }

    @Override
    public void saveOrderCreatedOutbox(Orders order) {
        saveOutbox(AggregateType.ORDER, order.getId().toString(), OutboxEventType.ORDER_CREATED,
                new OutboxEntityPayload(
                        order.getStatus().getStatusCode(),

                        Optional.ofNullable(order.getItems())
                                .orElse(Collections.emptyList())
                                .stream()
                                .collect(Collectors.toMap(
                                        item -> item.getProductId().toString(),
                                        OrderItemEntity::getQuantity,
                                        Integer::sum
                                ))
                )
        );
    }
}