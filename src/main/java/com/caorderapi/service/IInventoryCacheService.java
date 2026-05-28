package com.caorderapi.service;

import com.caorderapi.dto.ProductCacheDto;

import java.util.UUID;

/**
 * Abstraction for a lightweight inventory cache (e.g. Redis) used for fast stock
 * reads and atomic reserve/release operations.
 */
public interface IInventoryCacheService {

    /**
     * Initialize or overwrite stock value for a product in the cache.
     */
    void setStock(UUID productId, String sku, String price, String stockQuantity);

    /**
     * Atomically reserve {@code qty} units of product. Returns true when reservation
     * succeeded, false when insufficient stock. Implementations may return false or
     * throw for missing product depending on policy.
     */
    boolean reserveStock(UUID productId, int qty);

    /**
     * Release previously reserved quantity back into the cached stock.
     */
    void releaseStock(UUID productId, int qty);

    /**
     * Read current stock value from the cache. Returns zero if not present.
     */
    Integer getStock(UUID productId);

    /**
     * Validates whether a product id is known to the cache/store. Returns true
     * when the product exists (cache entry present), false otherwise.
     */
    ProductCacheDto getProduct(UUID productId);
}
