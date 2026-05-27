package com.caorderapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID productId,
    Integer quantity,
    BigDecimal priceAtPurchaseCents,
    String status
) {
}

