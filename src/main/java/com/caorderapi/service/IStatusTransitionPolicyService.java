package com.caorderapi.service;

import com.caorderapi.model.OrderItemStatusEntity;
import com.caorderapi.model.OrderStatusEntity;

/**
 * Policy contract for schema-driven order status validation and transition rules.
 */
public interface IStatusTransitionPolicyService {

    /**
     * Returns an order status entity only if it exists and is active.
     */
    OrderStatusEntity requireActiveOrderStatus(String statusCode);

    /**
     * Retrieves an order status entity by code.
     */
    OrderStatusEntity getOrderStatus(String statusCode);

    /**
     * Retrieves an order item status entity by code.
     */
    OrderItemStatusEntity getOrderItemStatus(String statusCode);

    /**
     * Returns a status code only if it exists and is active.
     */
    String requireActiveStatus(String statusCode);

    /**
     * Evaluates whether a transition from current status to next status is permitted.
     */
    boolean isTransitionAllowed(String currentStatus, String nextStatus);

    /**
     * Ensures both statuses are active and the transition edge exists.
     * Throws {@link com.caorderapi.exception.InvalidOrderStateException} if not.
     */
    void assertTransitionAllowed(String currentStatus, String nextStatus);
}

