package com.caorderapi.integration;

import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.feign.FulfillmentGatewayClient;
import com.caorderapi.feign.PaymentGatewayClient;
import com.caorderapi.feign.dto.FulfillmentResponse;
import com.caorderapi.feign.dto.PaymentChargeResponse;
import com.caorderapi.model.ProductEntity;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IInventoryCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    private static final String BASE_URL = "/api/v1/orders";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;

    @MockitoBean private IInventoryCacheService inventoryCacheService;
    @MockitoBean private PaymentGatewayClient paymentGatewayClient;
    @MockitoBean private FulfillmentGatewayClient fulfillmentGatewayClient;

    private UUID productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        reset(inventoryCacheService, paymentGatewayClient, fulfillmentGatewayClient);

        productId = UUID.randomUUID();
        when(inventoryCacheService.getProduct(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    return new ProductCacheDto(id, "SKU-INT", BigDecimal.valueOf(49.99), 100);
                });
        when(inventoryCacheService.reserveStock(any(UUID.class), anyInt())).thenReturn(true);
        when(paymentGatewayClient.charge(any())).thenReturn(
                new PaymentChargeResponse("tx-1", "order-1", "AUTHORISED", null, "AUTH-1"));
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(new FulfillmentResponse("ACCEPTED", "ful-1"));

        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setSku("SKU-INT-" + productId);
        product.setName("Integration Product");
        product.setDescription("Seeded product for API integration test");
        product.setPrice(BigDecimal.valueOf(49.99));
        product.setStockQuantity(500);
        product.setActive(true);
        productRepository.save(product);
    }

    @Test
    void createOrder_happyPath_andIdempotent() throws Exception {
        long ordersBefore = orderRepository.count();
        String body = validCreateBody(productId);

        String first = mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-it-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-it-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode firstJson = objectMapper.readTree(first);
        JsonNode secondJson = objectMapper.readTree(second);
        String firstId = firstJson.get("id").asText();

        assertThat(secondJson.get("id").asText()).isEqualTo(firstId);
        assertThat(orderRepository.findById(UUID.fromString(firstId))).isPresent();
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);

        mockMvc.perform(get(BASE_URL + "/{id}", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_validationAndRequestErrors() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody(productId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));

        mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-invalid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"bad-email","currency":"EUR","items":[{"productId":"%s","quantity":2}]}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-invalid-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"integration@ca.com","currency":"eur","items":[{"productId":"%s","quantity":2}]}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-invalid-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"integration@ca.com","currency":"EUR","items":[]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-invalid-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void createOrder_stockFailure_returnsBadRequest() throws Exception {
        when(inventoryCacheService.reserveStock(any(UUID.class), anyInt())).thenReturn(false);

        mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", "idem-stock-fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody(productId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void payOrder_success_andRepeatedPayInvalidState() throws Exception {
        String orderId = createOrderAndGetId("idem-pay-1");

        mockMvc.perform(post(BASE_URL + "/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post(BASE_URL + "/{id}/pay", orderId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payOrder_notFound_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/pay", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void payOrder_externalPaymentFailure_returns502() throws Exception {
        when(paymentGatewayClient.charge(any()))
                .thenReturn(new PaymentChargeResponse("tx-2", "order-2", "REFUSED", "DECLINED", null));

        String orderId = createOrderAndGetId("idem-pay-2");

        mockMvc.perform(post(BASE_URL + "/{id}/pay", orderId))
                .andExpect(status().isBadGateway());

        mockMvc.perform(get(BASE_URL + "/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void transitionOrder_cancelled_success() throws Exception {
        String orderId = createOrderAndGetId("idem-cancel-1");

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void transitionOrder_paidToFulfilled_success() throws Exception {
        String orderId = createOrderAndGetId("idem-fulfill-1");

        mockMvc.perform(post(BASE_URL + "/{id}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void transitionOrder_notFound_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transitionOrder_invalidPayloadAndStatePaths() throws Exception {
        String orderId = createOrderAndGetId("idem-transition-invalid");

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transitionOrder_fulfillmentFailure_returns502() throws Exception {
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(new FulfillmentResponse("REJECTED", "ful-x"));

        String orderId = createOrderAndGetId("idem-fulfill-fail");
        mockMvc.perform(post(BASE_URL + "/{id}/pay", orderId)).andExpect(status().isOk());

        mockMvc.perform(post(BASE_URL + "/{id}/status/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"FULFILLED\"}"))
                .andExpect(status().isBadGateway());
    }

    private String createOrderAndGetId(String idempotencyKey) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody(productId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String validCreateBody(UUID pid) {
        return """
                {
                  "customerEmail": "integration@ca.com",
                  "currency": "EUR",
                  "items": [
                    {"productId": "%s", "quantity": 2}
                  ]
                }
                """.formatted(pid);
    }
}

