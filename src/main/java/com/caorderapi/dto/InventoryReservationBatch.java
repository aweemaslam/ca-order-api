package com.caorderapi.dto;

import com.caorderapi.model.InventoryReservationEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of inventory reservation preparation.
 */
public record InventoryReservationBatch(BigDecimal total, List<InventoryReservationEntity> reservations) {
}

