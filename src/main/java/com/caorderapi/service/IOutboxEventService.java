package com.caorderapi.service;

import com.caorderapi.model.OrderEntity;

public interface IOutboxEventService {
    void saveOrderCreatedOutbox(OrderEntity order);

    void saveStockReleaseOutbox(OrderEntity order);

    void saveStatusChangedOutbox(OrderEntity order);
}
