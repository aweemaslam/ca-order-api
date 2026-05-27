package com.caorderapi.feign.port;

import com.caorderapi.model.Orders;

public interface PaymentPort {
    void charge(Orders order);
}
