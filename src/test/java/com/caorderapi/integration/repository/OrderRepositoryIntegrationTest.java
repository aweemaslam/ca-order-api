package com.caorderapi.integration.repository;

import com.caorderapi.model.OrderStatusEntity;
import com.caorderapi.model.Orders;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.repository.OrderStatusRepository;
import com.caorderapi.service.IInventoryCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusRepository orderStatusRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IInventoryCacheService inventoryCacheService;

    private OrderStatusEntity pending;
    private OrderStatusEntity paid;

    @BeforeEach
    void setUp() {
        pending = orderStatusRepository.findById("PENDING")
                .orElseThrow(() -> new IllegalStateException("Missing PENDING status seed"));
        paid = orderStatusRepository.findById("PAID")
                .orElseThrow(() -> new IllegalStateException("Missing PAID status seed"));
    }

    @Test
    void findByIdempotencyKeyReturnsOrderWhenPresent() {
        Orders order = saveOrder("idem-integration-1", pending, Instant.now().minus(20, ChronoUnit.MINUTES));

        var found = orderRepository.findByIdempotencyKey("idem-integration-1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void findByIdempotencyKeyReturnsEmptyWhenMissing() {
        var found = orderRepository.findByIdempotencyKey("missing-key");

        assertThat(found).isEmpty();
    }

    @Test
    void findPendingOrdersOlderThanFiltersByStatusAndCutoff() {
        Orders oldPending = saveOrder("idem-integration-2", pending, Instant.now().minus(30, ChronoUnit.MINUTES));
        Orders recentPending = saveOrder("idem-integration-3", pending, Instant.now().minus(5, ChronoUnit.MINUTES));
        Orders oldPaid = saveOrder("idem-integration-4", paid, Instant.now().minus(30, ChronoUnit.MINUTES));

        var page = orderRepository.findPendingOrdersOlderThan(
                "PENDING",
                Instant.now().plus(1, ChronoUnit.DAYS),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).contains(oldPending.getId());
        assertThat(page.getContent()).contains(recentPending.getId());
        assertThat(page.getContent()).doesNotContain(oldPaid.getId());
    }

    private Orders saveOrder(String idempotencyKey, OrderStatusEntity status, Instant createdAt) {
        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setCustomerEmail("integration@ca.com");
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(status);
        order.setCurrency("EUR");
        order.setTotalAmount(BigDecimal.valueOf(19.99));
        order.setActive(true);
        Orders saved = orderRepository.save(order);
        jdbcTemplate.update("update orders set created_at = ? where order_id = ?", createdAt, saved.getId());
        return saved;
    }
}

