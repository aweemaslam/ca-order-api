package com.caorderapi.feign.port;

import java.util.UUID;

/**
 * Outbound port for fulfillment processing.
 */
public interface FulfillmentPort {

    /**
     * Triggers lightweight fulfillment for an already paid order.
     *
     * @param orderId order identifier
     */
    void fulfill(UUID orderId);
}

