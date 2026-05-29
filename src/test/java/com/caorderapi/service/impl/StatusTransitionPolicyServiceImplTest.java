package com.caorderapi.service.impl;

import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.model.OrderItemStatusEntity;
import com.caorderapi.model.OrderStatusEntity;
import com.caorderapi.repository.OrderItemStatusRepository;
import com.caorderapi.repository.OrderStatusRepository;
import com.caorderapi.repository.UpdateStatusRestrictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusTransitionPolicyServiceImplTest {

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private UpdateStatusRestrictionRepository updateStatusRestrictionRepository;

    @Mock
    private OrderItemStatusRepository orderItemStatusRepository;

    @InjectMocks
    private StatusTransitionPolicyServiceImpl service;

    @Test
    void requireActiveStatusReturnsStatusCodeWhenPresent() {
        OrderStatusEntity entity = new OrderStatusEntity();
        entity.setStatusCode("PENDING");
        entity.setActive(true);

        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PENDING")).thenReturn(Optional.of(entity));

        String result = service.requireActiveStatus("PENDING");

        assertEquals("PENDING", result);
    }

    @Test
    void requireActiveStatusThrowsWhenMissing() {
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(InvalidOrderStateException.class, () -> service.requireActiveStatus("UNKNOWN"));
    }

    @Test
    void requireActiveStatusNormalizesInput() {
        OrderStatusEntity entity = new OrderStatusEntity();
        entity.setStatusCode("PENDING");
        entity.setActive(true);
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PENDING")).thenReturn(Optional.of(entity));

        String result = service.requireActiveStatus(" pending ");

        assertEquals("PENDING", result);
    }

    @Test
    void requireActiveOrderStatusReturnsEntity() {
        OrderStatusEntity entity = new OrderStatusEntity();
        entity.setStatusCode("PAID");
        entity.setActive(true);
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PAID")).thenReturn(Optional.of(entity));

        assertEquals(entity, service.requireActiveOrderStatus("PAID"));
    }

    @Test
    void requireActiveOrderStatusThrowsWhenMissing() {
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PAID")).thenReturn(Optional.empty());

        assertThrows(InvalidOrderStateException.class, () -> service.requireActiveOrderStatus("PAID"));
    }

    @Test
    void getOrderStatusReturnsEntity() {
        OrderStatusEntity entity = new OrderStatusEntity();
        entity.setStatusCode("FULFILLED");
        entity.setActive(true);
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("FULFILLED")).thenReturn(Optional.of(entity));

        assertEquals(entity, service.getOrderStatus("FULFILLED"));
    }

    @Test
    void getOrderStatusThrowsWhenMissing() {
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("FULFILLED")).thenReturn(Optional.empty());

        assertThrows(InvalidOrderStateException.class, () -> service.getOrderStatus("FULFILLED"));
    }

    @Test
    void getOrderItemStatusReturnsEntity() {
        OrderItemStatusEntity entity = new OrderItemStatusEntity();
        entity.setStatusCode("CONFIRMED");
        entity.setActive(true);
        when(orderItemStatusRepository.findByStatusCodeAndActiveTrue("CONFIRMED")).thenReturn(Optional.of(entity));

        assertEquals(entity, service.getOrderItemStatus("CONFIRMED"));
    }

    @Test
    void getOrderItemStatusThrowsWhenMissing() {
        when(orderItemStatusRepository.findByStatusCodeAndActiveTrue("CONFIRMED")).thenReturn(Optional.empty());

        assertThrows(InvalidOrderStateException.class, () -> service.getOrderItemStatus("CONFIRMED"));
    }

    @Test
    void isTransitionAllowedReturnsRepositoryDecision() {
        when(updateStatusRestrictionRepository.existsByCurrentStatusAndAllowedNextStatusAndActiveTrue("PENDING", "PAID"))
            .thenReturn(true);

        assertTrue(service.isTransitionAllowed("PENDING", "PAID"));

        when(updateStatusRestrictionRepository.existsByCurrentStatusAndAllowedNextStatusAndActiveTrue("PENDING", "FULFILLED"))
            .thenReturn(false);

        assertFalse(service.isTransitionAllowed("PENDING", "FULFILLED"));
    }

    @Test
    void assertTransitionAllowedThrowsWhenEdgeMissing() {
        OrderStatusEntity pending = new OrderStatusEntity();
        pending.setStatusCode("PENDING");
        pending.setActive(true);
        OrderStatusEntity fulfilled = new OrderStatusEntity();
        fulfilled.setStatusCode("FULFILLED");
        fulfilled.setActive(true);

        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PENDING")).thenReturn(Optional.of(pending));
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("FULFILLED")).thenReturn(Optional.of(fulfilled));
        when(updateStatusRestrictionRepository.existsByCurrentStatusAndAllowedNextStatusAndActiveTrue("PENDING", "FULFILLED"))
            .thenReturn(false);

        assertThrows(InvalidOrderStateException.class,
            () -> service.assertTransitionAllowed("PENDING", "FULFILLED"));
    }

    @Test
    void assertTransitionAllowedWhenEdgePresentDoesNotThrow() {
        OrderStatusEntity pending = new OrderStatusEntity();
        pending.setStatusCode("PENDING");
        pending.setActive(true);
        OrderStatusEntity paid = new OrderStatusEntity();
        paid.setStatusCode("PAID");
        paid.setActive(true);

        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PENDING")).thenReturn(Optional.of(pending));
        when(orderStatusRepository.findByStatusCodeAndActiveTrue("PAID")).thenReturn(Optional.of(paid));
        when(updateStatusRestrictionRepository.existsByCurrentStatusAndAllowedNextStatusAndActiveTrue("PENDING", "PAID"))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.assertTransitionAllowed("PENDING", "PAID"));
    }
}

