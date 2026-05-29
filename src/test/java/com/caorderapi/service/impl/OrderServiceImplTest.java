package com.caorderapi.service.impl;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.OrderResponse;
import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.exception.InsufficientStockException;
import com.caorderapi.exception.ResourceNotFoundException;
import com.caorderapi.feign.port.FulfillmentPort;
import com.caorderapi.feign.port.PaymentPort;
import com.caorderapi.model.Orders;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IOutboxEventService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import com.caorderapi.service.impl.OrderServiceImpl;
import com.caorderapi.service.mapper.OrderMapper;
import com.caorderapi.util.OrderTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentPort paymentPort;
    @Mock private FulfillmentPort fulfillmentPort;
    @Mock private OrderMapper orderMapper;
    @Mock private IOrderInventoryService orderInventoryService;
    @Mock private IStatusTransitionPolicyService orderStatusPolicyService;
    @Mock private ApplicationStatusConfigurations applicationStatusConfigurations;
    @Mock private IOutboxEventService outboxEventService;

    @InjectMocks
    private OrderServiceImpl service;

    private ApplicationStatusConfigurations.StatusConfig ordersConfig;
    private ApplicationStatusConfigurations.StatusConfig itemsConfig;

    @BeforeEach
    void setUp() {
        ordersConfig = new ApplicationStatusConfigurations.StatusConfig();
        ordersConfig.setInitialStatus("PENDING");
        ordersConfig.setPayTargetStatus("PAID");
        ordersConfig.setFulfillTargetStatus("FULFILLED");
        ordersConfig.setCancelledStatus("CANCELLED");

        itemsConfig = new ApplicationStatusConfigurations.StatusConfig();
        itemsConfig.setInitialStatus("PENDING");
        itemsConfig.setPayTargetStatus("CONFIRMED");
        itemsConfig.setFulfillTargetStatus("FULFILLED");
        itemsConfig.setCancelledStatus("CANCELLED");
    }

    private void stubAppStatuses() {
        lenient().when(applicationStatusConfigurations.getOrders()).thenReturn(ordersConfig);
        lenient().when(applicationStatusConfigurations.getOrderItems()).thenReturn(itemsConfig);
    }

    // ── createOrder ─────────────────────────────────────────────────────────────

    @Test
    void createOrder_newOrder_savesAndPublishesOutbox() {
        stubAppStatuses();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();
        Orders order = OrderTestFactory.pendingOrder();

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderStatusPolicyService.requireActiveOrderStatus("PENDING"))
                .thenReturn(OrderTestFactory.pendingStatus());
        when(orderInventoryService.reserveInventory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(99.98));
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(order.getId(), "PENDING"));

        OrderResponse result = service.createOrder(request, "idem-001");

        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Orders.class));
        verify(outboxEventService).saveOrderCreatedOutbox(any(Orders.class));
    }

    @Test
    void createOrder_idempotentById_returnsExisting() {
        Orders existing = OrderTestFactory.pendingOrder();
        CreateOrderRequest request = new CreateOrderRequest(
                existing.getId(), OrderTestFactory.CUSTOMER_EMAIL, "EUR",
                List.of(new CreateOrderItemRequest(OrderTestFactory.PRODUCT_ID, 1)));

        when(orderRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(orderMapper.toResponse(existing)).thenReturn(stubResponse(existing.getId(), "PENDING"));

        OrderResponse result = service.createOrder(request, "any-key");

        assertThat(result.id()).isEqualTo(existing.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_idempotentByKey_returnsExisting() {
        Orders existing = OrderTestFactory.pendingOrder();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();

        when(orderRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.of(existing));
        when(orderMapper.toResponse(existing)).thenReturn(stubResponse(existing.getId(), "PENDING"));

        OrderResponse result = service.createOrder(request, "idem-001");

        assertThat(result.id()).isEqualTo(existing.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_whenInventoryThrows_publishesStockReleaseOutbox() {
        stubAppStatuses();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderStatusPolicyService.requireActiveOrderStatus("PENDING"))
                .thenReturn(OrderTestFactory.pendingStatus());
        when(orderInventoryService.reserveInventory(any(), any(), any(), any()))
                .thenThrow(new InsufficientStockException("No stock"));

        assertThatThrownBy(() -> service.createOrder(request, "key-1"))
                .isInstanceOf(InsufficientStockException.class);

        verify(outboxEventService).saveStockReleaseOutbox(any(Orders.class));
    }

    @Test
    void createOrder_whenSaveFails_publishesStockReleaseOutbox() {
        stubAppStatuses();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderStatusPolicyService.requireActiveOrderStatus("PENDING"))
                .thenReturn(OrderTestFactory.pendingStatus());
        when(orderInventoryService.reserveInventory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.TEN);
        when(orderRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.createOrder(request, "key-2"))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(outboxEventService).saveStockReleaseOutbox(any(Orders.class));
    }

    @Test
    void createOrder_nullOrderId_generatesUUID() {
        stubAppStatuses();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderStatusPolicyService.requireActiveOrderStatus("PENDING"))
                .thenReturn(OrderTestFactory.pendingStatus());
        when(orderInventoryService.reserveInventory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.TEN);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(UUID.randomUUID(), "PENDING"));

        service.createOrder(request, "key");

        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();
    }

    @Test
    void createOrder_blankIdempotencyKey_treatedAsNull() {
        stubAppStatuses();
        CreateOrderRequest request = OrderTestFactory.createOrderRequest();

        when(orderStatusPolicyService.requireActiveOrderStatus("PENDING"))
                .thenReturn(OrderTestFactory.pendingStatus());
        when(orderInventoryService.reserveInventory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.TEN);
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(UUID.randomUUID(), "PENDING"));

        // Blank key should not trigger idempotency lookup
        service.createOrder(request, "   ");

        verify(orderRepository, never()).findByIdempotencyKey(any());
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isNull();
    }

    // ── getOrder ─────────────────────────────────────────────────────────────────

    @Test
    void getOrder_found_returnsMappedResponse() {
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(stubResponse(order.getId(), "PENDING"));

        OrderResponse result = service.getOrder(order.getId());

        assertThat(result.id()).isEqualTo(order.getId());
    }

    @Test
    void getOrder_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── payOrder ─────────────────────────────────────────────────────────────────

    @Test
    void payOrder_delegates_toTransitionWithPaidStatus() {
        stubAppStatuses();
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusPolicyService.requireActiveStatus("PAID")).thenReturn("PAID");
        when(orderStatusPolicyService.getOrderItemStatus("CONFIRMED"))
                .thenReturn(OrderTestFactory.confirmedItemStatus());
        when(orderStatusPolicyService.getOrderStatus("PAID")).thenReturn(OrderTestFactory.paidStatus());
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(order.getId(), "PAID"));
        doNothing().when(paymentPort).charge(any());
        doNothing().when(orderStatusPolicyService).assertTransitionAllowed(any(), any());

        OrderResponse result = service.payOrder(order.getId());

        assertThat(result.status()).isEqualTo("PAID");
    }

    // ── transitionOrderStatus ─────────────────────────────────────────────────────

    @Test
    void transitionOrderStatus_toPaid_chargesPaymentAndUpdatesItems() {
        stubAppStatuses();
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusPolicyService.requireActiveStatus("PAID")).thenReturn("PAID");
        when(orderStatusPolicyService.getOrderItemStatus("CONFIRMED"))
                .thenReturn(OrderTestFactory.confirmedItemStatus());
        when(orderStatusPolicyService.getOrderStatus("PAID")).thenReturn(OrderTestFactory.paidStatus());
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(order.getId(), "PAID"));
        doNothing().when(paymentPort).charge(any());
        doNothing().when(orderStatusPolicyService).assertTransitionAllowed(any(), any());

        service.transitionOrderStatus(order.getId(), "PAID");

        verify(paymentPort).charge(order);
        order.getItems().forEach(item ->
                assertThat(item.getStatus().getStatusCode()).isEqualTo("CONFIRMED"));
    }

    @Test
    void transitionOrderStatus_toFulfilled_dispatchesFulfillmentAndUpdatesItems() {
        stubAppStatuses();
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusPolicyService.requireActiveStatus("FULFILLED")).thenReturn("FULFILLED");
        when(orderStatusPolicyService.getOrderItemStatus("FULFILLED"))
                .thenReturn(OrderTestFactory.fulfilledItemStatus());
        when(orderStatusPolicyService.getOrderStatus("FULFILLED")).thenReturn(OrderTestFactory.fulfilledStatus());
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(order.getId(), "FULFILLED"));
        doNothing().when(fulfillmentPort).fulfill(any());
        doNothing().when(orderStatusPolicyService).assertTransitionAllowed(any(), any());

        service.transitionOrderStatus(order.getId(), "FULFILLED");

        verify(fulfillmentPort).fulfill(order.getId());
        order.getItems().forEach(item ->
                assertThat(item.getStatus().getStatusCode()).isEqualTo("FULFILLED"));
    }

    @Test
    void transitionOrderStatus_toCancelled_updatesItemsOnly() {
        stubAppStatuses();
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusPolicyService.requireActiveStatus("CANCELLED")).thenReturn("CANCELLED");
        when(orderStatusPolicyService.getOrderItemStatus("CANCELLED"))
                .thenReturn(OrderTestFactory.cancelledItemStatus());
        when(orderStatusPolicyService.getOrderStatus("CANCELLED")).thenReturn(OrderTestFactory.cancelledStatus());
        when(orderMapper.toResponse(any())).thenReturn(stubResponse(order.getId(), "CANCELLED"));
        doNothing().when(orderStatusPolicyService).assertTransitionAllowed(any(), any());

        service.transitionOrderStatus(order.getId(), "CANCELLED");

        verify(paymentPort, never()).charge(any());
        verify(fulfillmentPort, never()).fulfill(any());
        order.getItems().forEach(item ->
                assertThat(item.getStatus().getStatusCode()).isEqualTo("CANCELLED"));
    }

    @Test
    void transitionOrderStatus_unknownStatus_throwsInvalidOrderStateException() {
        Orders order = OrderTestFactory.pendingOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusPolicyService.requireActiveStatus("UNKNOWN")).thenReturn("UNKNOWN");
        doNothing().when(orderStatusPolicyService).assertTransitionAllowed(any(), any());

        assertThatThrownBy(() -> service.transitionOrderStatus(order.getId(), "UNKNOWN"))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void transitionOrderStatus_orderNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transitionOrderStatus(id, "PAID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private OrderResponse stubResponse(UUID id, String status) {
        return new OrderResponse(id, OrderTestFactory.CUSTOMER_EMAIL, status,
                BigDecimal.valueOf(99.98), "EUR", null, List.of());
    }
}

