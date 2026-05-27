package com.caorderapi.service;

import com.caorderapi.exception.InvalidOrderStateException;
import com.caorderapi.model.OrderStatusEntity;
import com.caorderapi.repository.OrderStatusRepository;
import com.caorderapi.repository.UpdateStatusRestrictionRepository;
import com.caorderapi.service.impl.StatusTransitionPolicyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusPolicyServiceTest {

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private UpdateStatusRestrictionRepository updateStatusRestrictionRepository;

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
}

