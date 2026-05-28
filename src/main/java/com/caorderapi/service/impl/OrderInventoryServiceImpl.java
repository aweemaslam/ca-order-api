package com.caorderapi.service.impl;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.exception.InsufficientStockException;
import com.caorderapi.model.OrderItemEntity;
import com.caorderapi.model.Orders;
import com.caorderapi.service.IInventoryCacheService;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default inventory workflow implementation using atomic DB updates + reservation records.
 */
@Service
@RequiredArgsConstructor
public class OrderInventoryServiceImpl implements IOrderInventoryService {

    private final IInventoryCacheService redisInventoryCacheService;
    private final IStatusTransitionPolicyService orderStatusPolicyService;

    @Override
    @Transactional
    public BigDecimal reserveInventory(
            Orders order,
            List<CreateOrderItemRequest> itemRequests,
            long reservationTtlMinutes,
            String initialItemStatus, String idempotencyKey) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : itemRequests) {
            ProductCacheDto validProduct = redisInventoryCacheService.getProduct(itemRequest.productId());
            boolean reserved = redisInventoryCacheService.reserveStock(
                    itemRequest.productId(),
                    itemRequest.quantity()
            );
            if (!reserved) {
                throw new InsufficientStockException("Insufficient stock for product SKU: " + validProduct.sku());
            }

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductId(validProduct.productId());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(validProduct.price());
            item.setActive(true);
            item.setStatus(orderStatusPolicyService.getOrderItemStatus(initialItemStatus));
            order.getItems().add(item);

            totalAmount = totalAmount.add(validProduct.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        return totalAmount;
    }
}





