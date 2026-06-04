package com.caorderapi.dto;

import java.util.UUID;

public record ProductReservedQuantityDto(
         UUID productId,
         Long reservedQuantity
) {
}

