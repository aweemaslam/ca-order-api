package com.caorderapi.feign;

import com.caorderapi.feign.config.FulfillmentFeignConfig;
import com.caorderapi.feign.dto.FulfillmentRequest;
import com.caorderapi.feign.dto.FulfillmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fulfillmentGatewayClient",
        url = "${app.fulfillment.base-url:http://localhost:8090}",
        configuration = FulfillmentFeignConfig.class
)
public interface FulfillmentGatewayClient {

    @PostMapping("/fulfillments/dispatches")
    FulfillmentResponse dispatch(@RequestBody FulfillmentRequest request);
}

