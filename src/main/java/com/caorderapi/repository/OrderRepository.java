package com.caorderapi.repository;

import com.caorderapi.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
	Optional<Orders> findByIdempotencyKey(String idempotencyKey);
}

