package com.caorderapi.service;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.model.Orders;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates inventory reservation and release operations for orders.
 */
public interface IOrderInventoryService {

    /**
     * Reserves stock, attaches order items, and prepares reservation records together with the calculated order total.
     */
    BigDecimal reserveInventory(
            Orders order,
            List<CreateOrderItemRequest> itemRequests,
            String initialStatus, String idempotencyKey);
}





