package com.caorderapi.service.mapper;

import com.caorderapi.dto.OrderResponse;
import com.caorderapi.model.Orders;
import com.caorderapi.util.OrderTestFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toResponseMapsTopLevelFields() {
        Orders order = OrderTestFactory.pendingOrder();

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.customerEmail()).isEqualTo(order.getCustomerEmail());
        assertThat(response.status()).isEqualTo(order.getStatus().getStatusCode());
        assertThat(response.totalAmountCents()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(response.currency()).isEqualTo(order.getCurrency());
        assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void toResponseMapsItems() {
        Orders order = OrderTestFactory.pendingOrder();

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.items()).hasSize(1);
        var item = response.items().getFirst();
        assertThat(item.productId()).isEqualTo(OrderTestFactory.PRODUCT_ID);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.priceAtPurchaseCents()).isEqualByComparingTo("49.99");
        assertThat(item.status()).isEqualTo("PENDING");
    }

    @Test
    void toResponseReturnsEmptyItemsForEmptyOrderItems() {
        Orders order = OrderTestFactory.pendingOrder();
        order.setItems(new ArrayList<>());

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.items()).isEmpty();
    }
}

