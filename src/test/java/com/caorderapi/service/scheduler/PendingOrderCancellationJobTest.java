package com.caorderapi.service.scheduler;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.service.IOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingOrderCancellationJobTest {

    @Mock private ApplicationStatusConfigurations applicationStatusConfigurations;
    @Mock private IOrderService orderService;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private PendingOrderCancellationJob job;

    private ApplicationStatusConfigurations.StatusConfig ordersConfig;

    @BeforeEach
    void setUp() {
        ordersConfig = new ApplicationStatusConfigurations.StatusConfig();
        ordersConfig.setInitialStatus("PENDING");
        ordersConfig.setCancelledStatus("CANCELLED");
        when(applicationStatusConfigurations.getOrders()).thenReturn(ordersConfig);
        // set private field reservationTtlMinutes
        ReflectionTestUtils.setField(job, "pendingOrderCancellationMs", 15);
    }

    @Test
    void publish_noPendingOrders_doesNothing() {
        when(orderRepository.findPendingOrdersOlderThan(eq("PENDING"), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        job.publish();

        verify(orderService, never()).transitionOrderStatus(any(), any());
    }

    @Test
    void publish_pendingOrders_cancelledEach() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        when(orderRepository.findPendingOrdersOlderThan(eq("PENDING"), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(id1, id2, id3)));

        job.publish();

        verify(orderService, times(3)).transitionOrderStatus(any(UUID.class), eq("CANCELLED"));
    }

    @Test
    void publish_paginates_untilNoPendingOrders() {
        UUID id1 = UUID.randomUUID();
        // First call returns a page with content but hasNext=false (single page)
        when(orderRepository.findPendingOrdersOlderThan(eq("PENDING"), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(id1)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        job.publish();

        verify(orderService, times(1)).transitionOrderStatus(id1, "CANCELLED");
    }
}
