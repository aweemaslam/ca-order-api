package com.caorderapi.service.impl;

import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.exception.ProductNotFoundException;
import com.caorderapi.model.ProductEntity;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IInventoryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisInventoryCacheService implements IInventoryCacheService {
    private static final String PRODUCT_KEY = "product:";
    private static final int PAGE_SIZE = 200;

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void setStock(UUID productId, String sku, String price, String stockQuantity) {
        String key = PRODUCT_KEY + productId;
        redisTemplate.opsForHash().put(key, "productId", productId.toString());
        redisTemplate.opsForHash().put(key, "sku", sku);
        redisTemplate.opsForHash().put(key, "price", price);
        redisTemplate.opsForHash().put(key, "stockQuantity", stockQuantity);
    }

    // ATOMIC RESERVE (Lua script)
    @Override
    public boolean reserveStock(UUID productId, int qty) {

        String key = PRODUCT_KEY + productId;

        String script =
                "local stock = tonumber(redis.call('HGET', KEYS[1], 'stockQuantity')) " +
                        "if stock == nil then return -1 end " +
                        "if stock < tonumber(ARGV[1]) then return 0 end " +
                        "redis.call('HINCRBY', KEYS[1], 'stockQuantity', -ARGV[1]) " +
                        "return 1";

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                List.of(key),
                String.valueOf(qty)
        );

        return result != null && result == 1;
    }

    @Override
    public void releaseStock(UUID productId, int qty) {

        String key = PRODUCT_KEY + productId;
        if (!redisTemplate.hasKey(key)) {
            throw new ProductNotFoundException("Product not found with id: %s".formatted(productId.toString()));
        }
        redisTemplate.opsForHash()
                .increment(key, "stockQuantity", qty);
    }

    @Override
    public Integer getStock(UUID productId) {

        String key = PRODUCT_KEY + productId;
        if (!redisTemplate.hasKey(key)) {
            throw new ProductNotFoundException("Product not found with id: %s".formatted(productId.toString()));
        }
        Object stock = redisTemplate.opsForHash()
                .get(key, "stockQuantity");

        return stock == null ? 0 : Integer.parseInt(stock.toString());
    }

    @Override
    public ProductCacheDto getProduct(UUID productId) {
        String key = PRODUCT_KEY + productId;

        if (!redisTemplate.hasKey(key)) {
             throw new ProductNotFoundException("Product not found with id: %s".formatted(productId.toString()));
        }

        Object stockObj = redisTemplate.opsForHash().get(key, "stockQuantity");

        return new ProductCacheDto(UUID.fromString(
                (String) Objects.requireNonNull(redisTemplate.opsForHash().get(key, "productId"))
        ), (String) redisTemplate.opsForHash().get(key, "sku"), new java.math.BigDecimal(
                (String) Objects.requireNonNull(redisTemplate.opsForHash().get(key, "price"))
        ), stockObj == null ? 0 : Integer.parseInt(stockObj.toString()));

    }


    @EventListener(ApplicationReadyEvent.class)
    private void loadProducts() {
        int page = 0;
        Page<ProductEntity> productPage;

        do {
            productPage = productRepository.findAll(PageRequest.of(page, PAGE_SIZE));
             for (ProductEntity product : productPage.getContent()) {
                 setStock(product.getId(), product.getSku(), product.getPrice().toPlainString(), String.valueOf(product.getStockQuantity()));
             }
             page++;
         } while (productPage.hasNext());
      }

}