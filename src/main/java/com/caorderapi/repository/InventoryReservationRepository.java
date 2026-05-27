package com.caorderapi.repository;

import com.caorderapi.enums.InventoryReservationStatus;
import com.caorderapi.model.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {
    List<InventoryReservationEntity> findByOrderIdAndStatus(UUID orderId, InventoryReservationStatus status);

    List<InventoryReservationEntity> findByOrderIdAndStatusNot(UUID orderId, InventoryReservationStatus status);
}


