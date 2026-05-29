package com.caorderapi.feign.adapter;

import com.caorderapi.exception.ExternalServiceException;
import com.caorderapi.feign.FulfillmentGatewayClient;
import com.caorderapi.feign.dto.FulfillmentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignFulfillmentAdapterTest {

    @Mock
    private FulfillmentGatewayClient fulfillmentGatewayClient;

    @InjectMocks
    private FeignFulfillmentAdapter adapter;

    @Test
    void fulfillSucceedsWhenProviderAccepts() {
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(new FulfillmentResponse("ACCEPTED", "ful-1"));

        assertDoesNotThrow(() -> adapter.fulfill(UUID.randomUUID()));
    }

    @Test
    void fulfillThrowsWhenProviderRejects() {
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(new FulfillmentResponse("REJECTED", "ful-2"));

        assertThrows(ExternalServiceException.class,
                () -> adapter.fulfill(UUID.randomUUID()));
    }

    @Test
    void fulfillThrowsWhenResponseIsNull() {
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(null);

        assertThrows(ExternalServiceException.class,
                () -> adapter.fulfill(UUID.randomUUID()));
    }

    @Test
    void fulfillThrowsWhenStatusIsNull() {
        when(fulfillmentGatewayClient.dispatch(any())).thenReturn(new FulfillmentResponse(null, "ful-3"));

        assertThrows(ExternalServiceException.class,
                () -> adapter.fulfill(UUID.randomUUID()));
    }
}

