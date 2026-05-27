package com.caorderapi.service.impl;

import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.model.OrderItemStatusEntity;
import com.caorderapi.model.OrderStatusEntity;
import com.caorderapi.repository.OrderItemStatusRepository;
import com.caorderapi.repository.OrderStatusRepository;
import com.caorderapi.repository.UpdateStatusRestrictionRepository;
import com.caorderapi.service.IStatusTransitionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Central policy service for schema-driven order status rules.
 */
@Service
@RequiredArgsConstructor
public class StatusTransitionPolicyServiceImpl implements IStatusTransitionPolicyService {

    private final OrderStatusRepository orderStatusRepository;
    private final OrderItemStatusRepository orderItemStatusRepository;

    private final UpdateStatusRestrictionRepository updateStatusRestrictionRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "activeOrderStatuses", key = "#statusCode")
    public OrderStatusEntity requireActiveOrderStatus(String statusCode) {
        return orderStatusRepository.findByStatusCodeAndActiveTrue(statusCode)
                .orElseThrow(() -> new InvalidOrderStateException("Status is not active or does not exist: " + statusCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "getOrderStatus", key = "#statusCode")
    public OrderStatusEntity getOrderStatus(String statusCode) {
        return orderStatusRepository.findByStatusCodeAndActiveTrue(statusCode)
                .orElseThrow(() -> new InvalidOrderStateException("Order status not found: " + statusCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "getOrderItemStatus", key = "#statusCode")
    public OrderItemStatusEntity getOrderItemStatus(String statusCode) {
        return orderItemStatusRepository.findByStatusCodeAndActiveTrue(statusCode)
                .orElseThrow(() -> new InvalidOrderStateException("Order Item status not found: " + statusCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "activeStatuses", key = "#statusCode")
    public String requireActiveStatus(String statusCode) {
        String normalizedTarget = statusCode.trim().toUpperCase(Locale.ROOT);
        return orderStatusRepository.findByStatusCodeAndActiveTrue(normalizedTarget)
                .map(OrderStatusEntity::getStatusCode)
                .orElseThrow(() -> new InvalidOrderStateException("Status is not active or does not exist: " + statusCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheNames = "allowedTransitions", key = "#currentStatus + '->' + #nextStatus")
    public boolean isTransitionAllowed(String currentStatus, String nextStatus) {
        return updateStatusRestrictionRepository
                .existsByCurrentStatusAndAllowedNextStatusAndActiveTrue(currentStatus, nextStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertTransitionAllowed(String currentStatus, String nextStatus) {
        requireActiveStatus(currentStatus);
        requireActiveStatus(nextStatus);
        if (!isTransitionAllowed(currentStatus, nextStatus)) {
            throw new InvalidOrderStateException(
                    "Transition is not allowed: " + currentStatus + " -> " + nextStatus);
        }
    }
}

