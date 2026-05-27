package com.caorderapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for customer orders.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@RequiredArgsConstructor
public class Orders extends BaseEntity implements Serializable {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code", nullable = false, foreignKey = @ForeignKey(name = "fk_order_statuses_code"))
    private OrderStatusEntity status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}