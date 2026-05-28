package com.caorderapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCacheDto(
         UUID productId,
         String sku,
         BigDecimal price,
         Integer stockQuantity
) {
}

