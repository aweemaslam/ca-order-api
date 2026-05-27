package com.caorderapi.repository;

import com.caorderapi.model.OrderStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStatusRepository extends JpaRepository<OrderStatusEntity, String> {
    Optional<OrderStatusEntity> findByStatusCodeAndActiveTrue(String statusCode);
}

