package com.caorderapi.repository;

import com.caorderapi.dto.ProductReservedQuantityDto;
import com.caorderapi.model.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {
    @Query("""
             select new com.caorderapi.dto.ProductReservedQuantityDto(
                 oi.productId,
                 sum(oi.quantity)
             )
             from OrderItemEntity oi
             where oi.status.statusCode = :status and oi.active=true
             group by oi.productId
            """)
    List<ProductReservedQuantityDto> findProductQuantitiesForStatus(@Param("status") String status);
}

