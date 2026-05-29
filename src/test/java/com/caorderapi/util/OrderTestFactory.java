package com.caorderapi.util;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared test data factory for consistent domain objects across all tests.
 */
public final class OrderTestFactory {

    public static final UUID ORDER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID PRODUCT_ID = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");
    public static final String CUSTOMER_EMAIL = "john@ca.com";
    public static final String CURRENCY = "EUR";
    public static final String IDEMPOTENCY_KEY = "idem-001";

    private OrderTestFactory() {}

    public static OrderStatusEntity pendingStatus() {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusCode("PENDING");
        status.setDescription("Order created, awaiting payment");
        status.setActive(true);
        return status;
    }

    public static OrderStatusEntity paidStatus() {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusCode("PAID");
        status.setDescription("Payment confirmed");
        status.setActive(true);
        return status;
    }

    public static OrderStatusEntity fulfilledStatus() {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusCode("FULFILLED");
        status.setDescription("Order dispatched");
        status.setActive(true);
        return status;
    }

    public static OrderStatusEntity cancelledStatus() {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusCode("CANCELLED");
        status.setDescription("Order cancelled");
        status.setActive(true);
        return status;
    }

    public static OrderItemStatusEntity pendingItemStatus() {
        OrderItemStatusEntity s = new OrderItemStatusEntity();
        s.setStatusCode("PENDING");
        s.setDescription("Item pending");
        s.setActive(true);
        return s;
    }

    public static OrderItemStatusEntity confirmedItemStatus() {
        OrderItemStatusEntity s = new OrderItemStatusEntity();
        s.setStatusCode("CONFIRMED");
        s.setDescription("Item confirmed");
        s.setActive(true);
        return s;
    }

    public static OrderItemStatusEntity cancelledItemStatus() {
        OrderItemStatusEntity s = new OrderItemStatusEntity();
        s.setStatusCode("CANCELLED");
        s.setDescription("Item cancelled");
        s.setActive(true);
        return s;
    }

    public static OrderItemStatusEntity fulfilledItemStatus() {
        OrderItemStatusEntity s = new OrderItemStatusEntity();
        s.setStatusCode("FULFILLED");
        s.setDescription("Item fulfilled");
        s.setActive(true);
        return s;
    }

    public static OrderItemEntity orderItem(Orders order) {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProductId(PRODUCT_ID);
        item.setQuantity(2);
        item.setPrice(BigDecimal.valueOf(49.99));
        item.setActive(true);
        item.setStatus(pendingItemStatus());
        return item;
    }

    public static Orders pendingOrder() {
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setCustomerEmail(CUSTOMER_EMAIL);
        order.setCurrency(CURRENCY);
        order.setIdempotencyKey(IDEMPOTENCY_KEY);
        order.setStatus(pendingStatus());
        order.setTotalAmount(BigDecimal.valueOf(99.98));
        order.setActive(true);
        order.setCreatedAt(Instant.now());
        order.setItems(new ArrayList<>(List.of(orderItem(order))));
        return order;
    }

    public static CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(
                null,
                CUSTOMER_EMAIL,
                CURRENCY,
                List.of(new CreateOrderItemRequest(PRODUCT_ID, 2))
        );
    }

    public static ProductCacheDto productCacheDto() {
        return new ProductCacheDto(PRODUCT_ID, "SKU-001", BigDecimal.valueOf(49.99), 100);
    }
}

