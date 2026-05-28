package com.caorderapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerEmail,
    String status,
    BigDecimal totalAmountCents,
    String currency,
    Instant createdAt,
    List<OrderItemResponse> items
) {
}

