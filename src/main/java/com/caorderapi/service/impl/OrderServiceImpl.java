package com.caorderapi.service.impl;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.OrderResponse;
import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.exception.ResourceNotFoundException;
import com.caorderapi.feign.port.FulfillmentPort;
import com.caorderapi.feign.port.PaymentPort;
import com.caorderapi.model.Orders;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IOrderService;
import com.caorderapi.service.IOutboxEventService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import com.caorderapi.service.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service implementing order lifecycle use cases.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;
    private final FulfillmentPort fulfillmentPort;
    private final OrderMapper orderMapper;
    private final IOrderInventoryService orderInventoryService;
    private final IStatusTransitionPolicyService orderStatusPolicyService;
    private final ApplicationStatusConfigurations applicationStatusConfigurations;
    private final IOutboxEventService outboxEventService;
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        UUID requestedOrderId = request.orderId();
        Orders order = new Orders();
        order.setId(requestedOrderId != null ? requestedOrderId : UUID.randomUUID());
        try {

            if (requestedOrderId != null) {
                Orders existingById = orderRepository.findById(requestedOrderId).orElse(null);
                if (existingById != null) {
                    return orderMapper.toResponse(existingById);
                }
            }

            String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
            if (normalizedKey != null) {
                Orders existing = orderRepository.findByIdempotencyKey(normalizedKey).orElse(null);
                if (existing != null) {
                    return orderMapper.toResponse(existing);
                }
            }

            order.setCustomerEmail(request.customerEmail());
            order.setCurrency(request.currency());
            order.setIdempotencyKey(normalizedKey);
            order.setStatus(orderStatusPolicyService.requireActiveOrderStatus(applicationStatusConfigurations.getOrders().getInitialStatus()));

            BigDecimal totalAmount = orderInventoryService.reserveInventory(
                    order,
                    request.items(),
                    applicationStatusConfigurations.getOrderItems().getInitialStatus(),
                    normalizedKey);

            order.setTotalAmount(totalAmount);
            order.setActive(true);
            order.setCreatedAt(Instant.now());
            orderRepository.save(order);
            outboxEventService.saveOrderCreatedOutbox(order);
            return orderMapper.toResponse(order);
        } catch (Exception e) {
            outboxEventService.saveStockReleaseOutbox(order);
            throw e;
        }
    }

    @Override
    @Cacheable(cacheNames = "ordersById", key = "#orderId")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "payOrder", key = "#orderId")
    public OrderResponse payOrder(UUID orderId) {
        return transitionOrderStatus(orderId, applicationStatusConfigurations.getOrders().getPayTargetStatus());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "transitionOrderStatus")
    public OrderResponse transitionOrderStatus(UUID orderId, String targetStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        String nextStatusCode = orderStatusPolicyService.requireActiveStatus(targetStatus);
        orderStatusPolicyService.assertTransitionAllowed(order.getStatus().getStatusCode(), nextStatusCode);

        switch (nextStatusCode.toUpperCase()) {
            case "PAID" -> {
                paymentPort.charge(order);
                order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                        .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getPayTargetStatus())));
            }
            case "FULFILLED" -> {

                fulfillmentPort.fulfill(orderId);
                order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                        .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getFulfillTargetStatus())));
            }
            case "CANCELLED" -> {
                order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                        .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getCancelledStatus())));
            }
            default -> throw new InvalidOrderStateException("Invalid Order Status, cannot transition to %s status"
                    .formatted(targetStatus));
        }

        order.setStatus(orderStatusPolicyService.getOrderStatus(nextStatusCode));
        orderRepository.save(order);
        outboxEventService.saveStatusChangedOutbox(order);
        return orderMapper.toResponse(order);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
