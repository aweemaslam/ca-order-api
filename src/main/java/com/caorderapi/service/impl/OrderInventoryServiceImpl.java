package com.caorderapi.service.impl;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.exception.InsufficientStockException;
import com.caorderapi.model.OrderEntity;
import com.caorderapi.model.OrderItemEntity;
import com.caorderapi.service.IInventoryCacheService;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default inventory workflow implementation for reservation of stock.
 */
@Service
@RequiredArgsConstructor
public class OrderInventoryServiceImpl implements IOrderInventoryService {

    private final IInventoryCacheService redisInventoryCacheService;
    private final IStatusTransitionPolicyService orderStatusPolicyService;

    @Override
    @Transactional
    public BigDecimal reserveInventory(
            OrderEntity order,
            List<CreateOrderItemRequest> itemRequests,
            String initialItemStatus, String idempotencyKey) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<UUID, Long> aggregatedItems = itemRequests.stream()
                .collect(Collectors.groupingBy(
                        CreateOrderItemRequest::productId,
                        Collectors.summingLong(CreateOrderItemRequest::quantity)
                ));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<UUID, Long> entry : aggregatedItems.entrySet()) {
            UUID productId = entry.getKey();
            Long totalQuantity = entry.getValue();
            ProductCacheDto validProduct = redisInventoryCacheService.getProduct(productId);
            boolean reserved = redisInventoryCacheService.reserveStock(
                    productId,
                    totalQuantity
            );
            if (!reserved) {
                throw new InsufficientStockException("Insufficient stock for product SKU: " + validProduct.sku());
            }

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductId(validProduct.productId());
            item.setQuantity(totalQuantity);
            item.setPrice(validProduct.price());
            item.setActive(true);
            item.setStatus(orderStatusPolicyService.getOrderItemStatus(initialItemStatus));
            order.getItems().add(item);

            totalAmount = totalAmount.add(validProduct.price().multiply(BigDecimal.valueOf(totalQuantity)));
        }

        return totalAmount;
    }
}





