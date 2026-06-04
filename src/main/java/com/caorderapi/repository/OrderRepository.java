package com.caorderapi.repository;

import com.caorderapi.model.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("select o.id from OrderEntity o where o.status.statusCode = :status and o.createdAt < :cutoffTime")
    Page<UUID> findPendingOrdersOlderThan(
            @Param("status") String status,
            @Param("cutoffTime") Instant cutoffTime, Pageable pageable
    );
}

