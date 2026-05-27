package com.caorderapi.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusTransitionRequest(
    @NotBlank(message = "targetStatus is required")
    String targetStatus
) {
}

