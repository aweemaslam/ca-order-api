package com.caorderapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    UUID orderId,

    @Email(message = "customerEmail must be a valid email")
    @NotBlank(message = "customerEmail is required")
    String customerEmail,

    @NotBlank(message = "currency is required")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "currency must be a valid ISO 4217 code"
    )
    String currency,

    @NotEmpty(message = "items cannot be empty")
    List<@Valid CreateOrderItemRequest> items
) {
}
