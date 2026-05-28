package com.caorderapi.feign.adapter;

import com.caorderapi.exception.ExternalServiceException;
import com.caorderapi.feign.PaymentGatewayClient;
import com.caorderapi.feign.dto.PaymentChargeRequest;
import com.caorderapi.feign.dto.PaymentChargeRequest.CaptureMode;
import com.caorderapi.feign.dto.PaymentChargeResponse;
import com.caorderapi.feign.port.PaymentPort;
import com.caorderapi.model.Orders;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignPaymentAdapter implements PaymentPort {

    private final PaymentGatewayClient paymentGatewayClient;

    @Override
    @CircuitBreaker(name = "paymentGatewayClient", fallbackMethod = "fallbackCharge")
    public void charge(Orders order) {
        // Convert decimal amount to smallest currency unit (cents) as required by Stripe/Adyen
        long amountCents = order.getTotalAmount().movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();

        PaymentChargeRequest request = new PaymentChargeRequest(
                order.getId().toString(),
                order.getId().toString(),
                amountCents,
                order.getCurrency(),
                order.getCustomerEmail(),
                "C&A Order #" + order.getId(),
                CaptureMode.IMMEDIATE
        );

        PaymentChargeResponse response = paymentGatewayClient.charge(request);

        if (response == null || response.resultCode() == null
                || !"AUTHORISED".equalsIgnoreCase(response.resultCode())) {
            String reason = response != null ? response.refusalReason() : "null response";
            throw new ExternalServiceException(
                    "Payment refused for order " + order.getId() + " – reason: " + reason, null);
        }

        log.info("Payment authorised for order={} transactionId={} authCode={}",
                order.getId(), response.transactionId(), response.authCode());
    }

    private void fallbackCharge(Orders order, Throwable ex) {
        log.error("Payment call failed for order={} amount={}", order.getId(), order.getTotalAmount(), ex);
        throw new ExternalServiceException(
                "Payment service temporarily unavailable for order " + order.getId(), ex);
    }
}
