package com.caorderapi.integration;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.feign.port.FulfillmentPort;
import com.caorderapi.feign.port.PaymentPort;
import com.caorderapi.repository.OrderItemStatusRepository;
import com.caorderapi.repository.OrderStatusRepository;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.repository.UpdateStatusRestrictionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    @Autowired
    private OrderItemStatusRepository orderItemStatusRepository;

    @Autowired
    private UpdateStatusRestrictionRepository updateStatusRestrictionRepository;

    @Autowired
    private ApplicationStatusConfigurations applicationStatusConfigurations;

    @MockitoBean
    private PaymentPort paymentPort;

    @MockitoBean
    private FulfillmentPort fulfillmentPort;

    @Test
    void createGetPayAndFulfillFlowWorks() throws Exception {
        String createPayload = """
                {
                  "customerEmail": "flow@ca.com",
                  "items": [
                    {"productId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", "quantity": 1}
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String orderId = createdJson.get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(paymentPort, times(1)).charge(any());

        mockMvc.perform(post("/api/v1/orders/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        verify(fulfillmentPort, times(1)).fulfill(UUID.fromString(orderId));
    }

    @Test
    void invalidTransitionReturnsBadRequest() throws Exception {
        String createPayload = """
                {
                  "customerEmail": "invalid-transition@ca.com",
                  "items": [
                    {"productId": "11111111-2222-3333-4444-555555555555", "quantity": 1}
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "transition-key")
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String orderId = createdJson.get("id").asText();

        mockMvc.perform(post("/api/v1/orders/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notFoundOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
