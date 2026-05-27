package com.caorderapi.service;

import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.OrderResponse;

import java.util.UUID;

/**
 * Primary inbound port for order lifecycle use cases.
 */
public interface IOrderService {

    /**
     * Creates a new order using an optional idempotency key for duplicate protection.
     */
    OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey);

    /**
     * Retrieves an order by id.
     */
    OrderResponse getOrder(UUID orderId);

    /**
     * Executes payment transition for an order.
     */
    OrderResponse payOrder(UUID orderId);

    /**
     * Applies a generic status transition guarded by DB transition rules.
     */
    OrderResponse transitionOrderStatus(UUID orderId, String targetStatus);
}
