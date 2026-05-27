package com.caorderapi.feign.adapter;

import com.caorderapi.exception.ExternalServiceException;
import com.caorderapi.feign.FulfillmentGatewayClient;
import com.caorderapi.feign.dto.FulfillmentRequest;
import com.caorderapi.feign.dto.FulfillmentResponse;
import com.caorderapi.feign.port.FulfillmentPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Outbound fulfillment adapter backed by Feign + circuit breaker.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeignFulfillmentAdapter implements FulfillmentPort {

    private final FulfillmentGatewayClient fulfillmentGatewayClient;

    /**
     * Calls fulfillment provider and validates dispatch response.
     */
    @Override
    @CircuitBreaker(name = "fulfillmentGatewayClient", fallbackMethod = "fallbackFulfill")
    public void fulfill(UUID orderId) {
        FulfillmentResponse response = fulfillmentGatewayClient.dispatch(new FulfillmentRequest(orderId.toString()));
        if (response == null || response.status() == null || !"ACCEPTED".equalsIgnoreCase(response.status())) {
            throw new ExternalServiceException("Fulfillment rejected for order " + orderId, null);
        }
        log.info("Fulfillment accepted for order={} referenceId={}", orderId, response.referenceId());
    }

    private void fallbackFulfill(UUID orderId, Throwable ex) {
        log.error("Fulfillment call failed for order={}", orderId, ex);
        throw new ExternalServiceException("Fulfillment service temporarily unavailable for order " + orderId, ex);
    }
}

