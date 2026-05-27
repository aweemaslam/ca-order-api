package com.caorderapi.feign;

import com.caorderapi.feign.config.PaymentGatewayFeignConfig;
import com.caorderapi.feign.dto.PaymentChargeRequest;
import com.caorderapi.feign.dto.PaymentChargeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymentGatewayClient",
        url = "${app.payment.base-url:http://localhost:8089}",
        configuration = PaymentGatewayFeignConfig.class
)
public interface PaymentGatewayClient {

    @PostMapping("/payments/charges")
    PaymentChargeResponse charge(@RequestBody PaymentChargeRequest request);
}

