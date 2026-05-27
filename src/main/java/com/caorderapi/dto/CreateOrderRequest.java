package com.caorderapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    UUID orderId,

    @Email(message = "customerEmail must be a valid email")
    @NotBlank(message = "customerEmail is required")
    String customerEmail,

    @NotEmpty(message = "items cannot be empty")
    List<@Valid CreateOrderItemRequest> items
) {
    public CreateOrderRequest(String customerEmail, List<CreateOrderItemRequest> items) {
        this(null, customerEmail, items);
    }
}

