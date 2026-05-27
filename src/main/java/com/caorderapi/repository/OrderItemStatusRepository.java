package com.caorderapi.repository;

import com.caorderapi.model.OrderItemStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing OrderItemStatusEntity lookup table operations.
 */
public interface OrderItemStatusRepository extends JpaRepository<OrderItemStatusEntity, String> {
    Optional<OrderItemStatusEntity> findByStatusCodeAndActiveTrue(String statusCode);
}

