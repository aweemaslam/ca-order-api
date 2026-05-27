package com.caorderapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerEmail,
    String status,
    BigDecimal totalAmountCents,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {
}

