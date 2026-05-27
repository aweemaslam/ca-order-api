package com.caorderapi.repository;

import com.caorderapi.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity p set p.stockQuantity = p.stockQuantity - :qty where p.id = :id and p.stockQuantity >= :qty")
    int decrementStockIfAvailable(@Param("id") UUID productId, @Param("qty") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity p set p.stockQuantity = p.stockQuantity + :qty where p.id = :id")
    int incrementStock(@Param("id") UUID productId, @Param("qty") int quantity);
}
