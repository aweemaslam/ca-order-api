package com.caorderapi.service.impl;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.exception.InsufficientStockException;
import com.caorderapi.exception.ProductNotFoundException;
import com.caorderapi.model.Orders;
import com.caorderapi.service.IInventoryCacheService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import com.caorderapi.util.OrderTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderInventoryServiceImplTest {

    @Mock private IInventoryCacheService inventoryCacheService;
    @Mock private IStatusTransitionPolicyService statusPolicyService;

    @InjectMocks private OrderInventoryServiceImpl service;

    @Test
    void reserveInventoryReturnsTotalAndAddsItems() {
        UUID productOne = UUID.randomUUID();
        UUID productTwo = UUID.randomUUID();
        when(inventoryCacheService.getProduct(productOne))
                .thenReturn(new ProductCacheDto(productOne, "SKU-1", BigDecimal.TEN, 10));
        when(inventoryCacheService.getProduct(productTwo))
                .thenReturn(new ProductCacheDto(productTwo, "SKU-2", BigDecimal.valueOf(5), 20));
        when(inventoryCacheService.reserveStock(productOne, 2)).thenReturn(true);
        when(inventoryCacheService.reserveStock(productTwo, 3)).thenReturn(true);
        when(statusPolicyService.getOrderItemStatus("PENDING")).thenReturn(OrderTestFactory.pendingItemStatus());

        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setItems(new ArrayList<>());

        BigDecimal total = service.reserveInventory(order, List.of(
                new CreateOrderItemRequest(productOne, 2),
                new CreateOrderItemRequest(productTwo, 3)
        ), "PENDING", "idem-1");

        assertThat(total).isEqualByComparingTo("35");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getStatus().getStatusCode()).isEqualTo("PENDING");
    }

    @Test
    void reserveInventoryThrowsWhenStockReservationFails() {
        UUID productId = UUID.randomUUID();
        when(inventoryCacheService.getProduct(productId))
                .thenReturn(new ProductCacheDto(productId, "SKU-404", BigDecimal.ONE, 0));
        when(inventoryCacheService.reserveStock(productId, 2)).thenReturn(false);

        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setItems(new ArrayList<>());

        assertThatThrownBy(() -> service.reserveInventory(order,
                List.of(new CreateOrderItemRequest(productId, 2)),
                "PENDING", "idem-2"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("SKU-404");
    }

    @Test
    void reserveInventoryPropagatesProductNotFound() {
        UUID productId = UUID.randomUUID();
        when(inventoryCacheService.getProduct(productId))
                .thenThrow(new ProductNotFoundException("missing"));

        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setItems(new ArrayList<>());

        assertThatThrownBy(() -> service.reserveInventory(order,
                List.of(new CreateOrderItemRequest(productId, 1)),
                "PENDING", "idem-3"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reserveInventoryMapsItemFields() {
        UUID productId = OrderTestFactory.PRODUCT_ID;
        ProductCacheDto product = OrderTestFactory.productCacheDto();
        when(inventoryCacheService.getProduct(productId)).thenReturn(product);
        when(inventoryCacheService.reserveStock(productId, 2)).thenReturn(true);
        when(statusPolicyService.getOrderItemStatus("PENDING")).thenReturn(OrderTestFactory.pendingItemStatus());

        Orders order = new Orders();
        order.setId(UUID.randomUUID());
        order.setItems(new ArrayList<>());

        service.reserveInventory(order,
                List.of(new CreateOrderItemRequest(productId, 2)),
                "PENDING", "idem-4");

        var item = order.getItems().getFirst();
        assertThat(item.getOrder()).isEqualTo(order);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPrice()).isEqualByComparingTo("49.99");
        assertThat(item.isActive()).isTrue();
    }
}

