package com.caorderapi.service;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.InventoryReservationBatch;
import com.caorderapi.model.InventoryReservationEntity;
import com.caorderapi.model.Orders;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulates inventory reservation and release operations for orders.
 */
public interface IOrderInventoryService {

    /**
     * Reserves stock, attaches order items, and prepares reservation records together with the calculated order total.
     */
    InventoryReservationBatch reserveInventory(
            Orders order,
            List<CreateOrderItemRequest> itemRequests,
            long reservationTtlMinutes,
            String initialStatus, String idempotencyKey);

    /**
     * Persists reservation rows after the order aggregate has been saved.
     */
    void saveReservations(List<InventoryReservationEntity> reservations);

    /**
     * Marks all active reservations for an order as confirmed.
     */
    void confirmReservations(UUID orderId);

    /**
     * Releases all active reservations for an order and returns stock to inventory.
     */
    void releaseReservations(UUID orderId);
}





