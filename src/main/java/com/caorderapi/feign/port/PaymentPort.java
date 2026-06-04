package com.caorderapi.feign.port;

import com.caorderapi.model.OrderEntity;

public interface PaymentPort {
    void charge(OrderEntity order);
}
