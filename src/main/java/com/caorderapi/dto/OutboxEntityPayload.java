package com.caorderapi.dto;

import java.io.Serializable;
import java.util.Map;

public record OutboxEntityPayload(
        String status,
        Map<String, Integer> productsWithQuantity
) implements Serializable {
}

