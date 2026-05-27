package com.caorderapi.controller;

import com.caorderapi.dto.OrderResponse;
import com.caorderapi.service.IOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderService orderService;

    @Test
    void createOrderReturnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.createOrder(any(), any())).thenReturn(response(orderId, "PENDING"));

        String payload = """
                {
                  "customerEmail": "john@ca.com",
                  "items": [
                    {"productId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", "quantity": 2}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-key")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrder(orderId)).thenReturn(response(orderId, "PAID"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void payOrderReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.payOrder(orderId)).thenReturn(response(orderId, "PAID"));

        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void transitionEndpointReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.transitionOrderStatus(eq(orderId), anyString())).thenReturn(response(orderId, "FULFILLED"));

        mockMvc.perform(post("/api/v1/orders/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void transitionEndpointValidatesPayload() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/orders/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private OrderResponse response(UUID orderId, String status) {
        return new OrderResponse(orderId, "john@ca.com", status, BigDecimal.valueOf(100), LocalDateTime.now(), List.of());
    }
}
