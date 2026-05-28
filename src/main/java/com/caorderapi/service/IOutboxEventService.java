package com.caorderapi.service;

import com.caorderapi.model.Orders;

public interface IOutboxEventService {
    void saveOrderCreatedOutbox(Orders order);

    void saveStockReleaseOutbox(Orders order);

    void saveStatusChangedOutbox(Orders order);
}
