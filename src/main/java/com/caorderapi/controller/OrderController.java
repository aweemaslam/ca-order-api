package com.caorderapi.controller;

import com.caorderapi.dto.CreateOrderRequest;
import com.caorderapi.dto.OrderResponse;
import com.caorderapi.dto.OrderStatusTransitionRequest;
import com.caorderapi.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for order lifecycle operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order APIs", description = "Create, retrieve and pay an order")
public class OrderController {

    private final IOrderService orderService;

    /**
     * Create a new order from catalog items.
     *
     * @param request Order create payload
     * @return Created order details
     */
    @PostMapping
    @Operation(summary = "Create order", description = "Create an order with one or more catalog items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                     @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        log.info("POST /api/v1/orders - creating order for customer={}", request.customerEmail());
        OrderResponse response = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get order by id.
     *
     * @param id Order identifier
     * @return Current order details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order", description = "Fetch order details by order id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        log.info("GET /api/v1/orders/{} - retrieving order", id);
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /**
     * Transition order from PENDING to PAID.
     *
     * @param id Order identifier
     * @return Updated order details
     */
    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay order", description = "Process lightweight payment for an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order paid successfully"),
            @ApiResponse(responseCode = "400", description = "Order not in payable state"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> payOrder(@PathVariable UUID id) {
        log.info("POST /api/v1/orders/{}/pay - processing payment", id);
        return ResponseEntity.ok(orderService.payOrder(id));
    }

    /**
     * Generic status transition endpoint using schema-driven transition rules.
     *
     * @param id      Order identifier
     * @param request Target status payload
     * @return Updated order details
     */
    @PostMapping("/{id}/status/transition")
    @Operation(summary = "Transition order status", description = "Transition order status using DB-configured allowed transitions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transition or payload"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> transitionOrderStatus(@PathVariable UUID id,
                                                               @Valid @RequestBody OrderStatusTransitionRequest request) {
        log.info("POST /api/v1/orders/{}/status/transition - targetStatus={}", id, request.targetStatus());
        return ResponseEntity.ok(orderService.transitionOrderStatus(id, request.targetStatus()));
    }
}