package com.caorderapi.feign.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

/**
 * Feign client-specific configuration for the Fulfillment Gateway.
 * NOTE: Must NOT be annotated with @Configuration to avoid registering beans
 * into the global application context (which would conflict with other Feign configs).
 * This class is referenced only via @FeignClient(configuration = ...).
 */
public class FulfillmentFeignConfig {

    /**
     * Defines retry policy with initial interval, max interval, and max attempts.
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 5);
    }
}