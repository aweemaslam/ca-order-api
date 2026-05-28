package com.caorderapi.service.mapper;

import com.caorderapi.dto.OrderItemResponse;
import com.caorderapi.dto.OrderResponse;
import com.caorderapi.model.OrderItemEntity;
import com.caorderapi.model.Orders;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Orders order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus().getStatusCode(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getQuantity(),
                item.getPrice(),
                item.getStatus().getStatusCode()
        );
    }
}
