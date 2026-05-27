package com.caorderapi.service.impl;

import com.caorderapi.dto.CreateOrderItemRequest;
import com.caorderapi.dto.InventoryReservationBatch;
import com.caorderapi.enums.InventoryReservationStatus;
import com.caorderapi.exception.InsufficientStockException;
import com.caorderapi.exception.ResourceNotFoundException;
import com.caorderapi.model.InventoryReservationEntity;
import com.caorderapi.model.OrderItemEntity;
import com.caorderapi.model.Orders;
import com.caorderapi.model.ProductEntity;
import com.caorderapi.repository.InventoryReservationRepository;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IOrderInventoryService;
import com.caorderapi.service.IStatusTransitionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default inventory workflow implementation using atomic DB updates + reservation records.
 */
@Service
@RequiredArgsConstructor
public class OrderInventoryServiceImpl implements IOrderInventoryService {

    private final ProductRepository productRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final IStatusTransitionPolicyService orderStatusPolicyService;

    @Override
    @Transactional
    public InventoryReservationBatch reserveInventory(
            Orders order,
            List<CreateOrderItemRequest> itemRequests,
            long reservationTtlMinutes,
            String initialItemStatus, String idempotencyKey) {
        Map<UUID, ProductEntity> productById = loadProducts(itemRequests);
        BigDecimal total = BigDecimal.ZERO;
        List<InventoryReservationEntity> reservations = new ArrayList<>();

        for (CreateOrderItemRequest itemRequest : itemRequests) {
            ProductEntity product = productById.get(itemRequest.productId());

            int updatedRows = productRepository.decrementStockIfAvailable(itemRequest.productId(), itemRequest.quantity());
            if (updatedRows == 0) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getSku());
            }

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setPrice(product.getPrice());
            item.setActive(true);
            item.setStatus(orderStatusPolicyService.getOrderItemStatus(initialItemStatus));
            order.getItems().add(item);

            InventoryReservationEntity reservation = new InventoryReservationEntity();
            reservation.setOrderId(order.getId());
            reservation.setProductId(product.getId());
            reservation.setQuantity(itemRequest.quantity());
            reservation.setStatus(InventoryReservationStatus.RESERVED);
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(reservationTtlMinutes));
            reservation.setIdempotencyKey(idempotencyKey);
            reservation.setActive(true);
            reservations.add(reservation);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        return new InventoryReservationBatch(total, reservations);
    }

    @Override
    public void saveReservations(List<InventoryReservationEntity> reservations) {
        inventoryReservationRepository.saveAll(reservations);
    }

    @Override
    public void confirmReservations(UUID orderId) {
        List<InventoryReservationEntity> reservations =
                inventoryReservationRepository.findByOrderIdAndStatus(orderId, InventoryReservationStatus.RESERVED);
        for (InventoryReservationEntity reservation : reservations) {
            reservation.setStatus(InventoryReservationStatus.CONFIRMED);
        }
        inventoryReservationRepository.saveAll(reservations);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseReservations(UUID orderId) {
        List<InventoryReservationEntity> reservations =
                inventoryReservationRepository.findByOrderIdAndStatusNot(orderId, InventoryReservationStatus.RELEASED);
        for (InventoryReservationEntity reservation : reservations) {
            int updatedRows = productRepository.incrementStock(reservation.getProductId(), reservation.getQuantity());
            if (updatedRows == 0) {
                throw new ResourceNotFoundException("Product not found during stock release: " + reservation.getProductId());
            }
            reservation.setStatus(InventoryReservationStatus.RELEASED);
            reservation.setActive(false);
        }
        inventoryReservationRepository.saveAll(reservations);
    }

    private Map<UUID, ProductEntity> loadProducts(List<CreateOrderItemRequest> itemRequests) {
        List<UUID> productIds = itemRequests.stream()
                .map(CreateOrderItemRequest::productId)
                .distinct()
                .toList();
        List<ProductEntity> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products do not exist");
        }
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, product -> product));
    }
}





