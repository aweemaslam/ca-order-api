package com.caorderapi.service.impl;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.OrderResponse;
import com.caorderapi.enums.AggregateType;
import com.caorderapi.enums.OutboxEventType;
import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.exception.ResourceNotFoundException;
import com.caorderapi.feign.port.FulfillmentPort;
import com.caorderapi.feign.port.PaymentPort;
import com.caorderapi.model.Orders;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.repository.OutboxEventRepository;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IOrderService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import com.caorderapi.service.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application service implementing order lifecycle use cases.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentPort paymentPort;
    private final FulfillmentPort fulfillmentPort;
    private final OrderMapper orderMapper;
    private final IOrderInventoryService orderInventoryService;
    private final IStatusTransitionPolicyService orderStatusPolicyService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationStatusConfigurations applicationStatusConfigurations;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        UUID requestedOrderId = request.orderId();
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

        Orders order = new Orders();
        order.setId(requestedOrderId != null ? requestedOrderId : UUID.randomUUID());
        order.setCustomerEmail(request.customerEmail());
        order.setCurrency(request.currency());
        order.setIdempotencyKey(normalizedKey);
        order.setStatus(orderStatusPolicyService.requireActiveOrderStatus(applicationStatusConfigurations.getOrders().getInitialStatus()));

        BigDecimal totalAmount = orderInventoryService.reserveInventory(
                order,
                request.items(),
                applicationStatusConfigurations.getOrders().getReservationTtlMinutes(),
                applicationStatusConfigurations.getOrderItems().getInitialStatus(),
                normalizedKey);

        order.setTotalAmount(totalAmount);
        order.setActive(true);

        orderRepository.save(order);
        saveOutbox(AggregateType.ORDER, order.getId().toString(), OutboxEventType.ORDER_CREATED,
                "{\"status\":\"" + order.getStatus().getStatusCode() + "\"}");
        return orderMapper.toResponse(order);
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
                try {
                    paymentPort.charge(order);
                    order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                            .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getPayTargetStatus())));
                } catch (Exception ex) {
                    throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
                }
            }
            case "FULFILLED" -> {
                try {
                    fulfillmentPort.fulfill(orderId);
                    order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                            .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getFulfillTargetStatus())));
                } catch (Exception ex) {
                    throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
                }
            }
            case "CANCELLED" -> {
                order.getItems().forEach(orderItem -> orderItem.setStatus(orderStatusPolicyService
                        .getOrderItemStatus(applicationStatusConfigurations.getOrderItems().getCancelledStatus())));

                saveOutbox(AggregateType.ORDER, orderId.toString(), OutboxEventType.STOCK_RELEASE_REQUESTED,
                        "{\"reason\":\"order_cancelled\"}");
            }
            default -> throw new InvalidOrderStateException("Invalid Order Status, cannot transition to %s status"
                    .formatted(targetStatus));
        }

        order.setStatus(orderStatusPolicyService.getOrderStatus(nextStatusCode));
        Orders saved = orderRepository.save(order);
        saveOutbox(AggregateType.ORDER, saved.getId().toString(), OutboxEventType.ORDER_STATUS_CHANGED,
                "{\"status\":\"" + saved.getStatus().getStatusCode() + "\"}");
        return orderMapper.toResponse(saved);
    }

    private void saveOutbox(AggregateType aggregateType, String aggregateId, OutboxEventType eventType, String payload) {
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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
