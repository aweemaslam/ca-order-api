package com.caorderapi.service;

import com.caorderapi.exception.ExternalServiceException;
import com.caorderapi.feign.PaymentGatewayClient;
import com.caorderapi.feign.adapter.FeignPaymentAdapter;
import com.caorderapi.feign.dto.PaymentChargeResponse;
import com.caorderapi.model.OrderStatusEntity;
import com.caorderapi.model.Orders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignPaymentAdapterTest {

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @InjectMocks
    private FeignPaymentAdapter adapter;

    private Orders buildOrder(BigDecimal amount) {
        OrderStatusEntity status = new OrderStatusEntity();
        status.setStatusCode("PENDING");
        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setCustomerEmail("customer@example.com");
        order.setTotalAmount(amount);
        order.setStatus(status);
        return order;
    }

    @Test
    void chargeSucceedsWhenProviderAuthorises() {
        when(paymentGatewayClient.charge(any()))
                .thenReturn(new PaymentChargeResponse("tx-1", "ref-1", "AUTHORISED", null, "AUTH123"));

        assertDoesNotThrow(() -> adapter.charge(buildOrder(BigDecimal.valueOf(49.99))));
    }

    @Test
    void chargeThrowsWhenProviderRefuses() {
        when(paymentGatewayClient.charge(any()))
                .thenReturn(new PaymentChargeResponse("tx-2", "ref-2", "REFUSED", "Insufficient funds", null));

        assertThrows(ExternalServiceException.class,
                () -> adapter.charge(buildOrder(BigDecimal.valueOf(49.99))));
    }
}
