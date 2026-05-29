package com.caorderapi.service.impl;

import com.caorderapi.dto.ProductCacheDto;
import com.caorderapi.exception.ProductNotFoundException;
import com.caorderapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisInventoryCacheServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks private RedisInventoryCacheService service;

    @Test
    void reserveStock_sufficientStock_returnsTrue() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(1L);
        assertThat(service.reserveStock(pid, 5)).isTrue();
    }

    @Test
    void reserveStock_insufficientStock_returnsFalse() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(0L);
        assertThat(service.reserveStock(pid, 5)).isFalse();
    }

    @Test
    void reserveStock_keyMissing_returnsFalse() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(-1L);
        assertThat(service.reserveStock(pid, 5)).isFalse();
    }

    @Test
    void reserveStock_nullResult_returnsFalse() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(null);
        assertThat(service.reserveStock(pid, 5)).isFalse();
    }

    @Test
    void releaseStock_keyMissing_throwsProductNotFoundException() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(false);
        assertThatThrownBy(() -> service.releaseStock(pid, 2))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void releaseStock_keyExists_incrementsStock() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.increment(anyString(), eq("stockQuantity"), eq(2L))).thenReturn(12L);

        service.releaseStock(pid, 2);

        verify(hashOperations).increment("product:" + pid, "stockQuantity", 2);
    }

    @Test
    void getProduct_keyMissing_throwsProductNotFoundException() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(false);
        assertThatThrownBy(() -> service.getProduct(pid))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProduct_keyExists_returnsMappedDto() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("product:" + pid, "stockQuantity")).thenReturn("50");
        when(hashOperations.get("product:" + pid, "productId")).thenReturn(pid.toString());
        when(hashOperations.get("product:" + pid, "sku")).thenReturn("SKU-001");
        when(hashOperations.get("product:" + pid, "price")).thenReturn("49.99");

        ProductCacheDto dto = service.getProduct(pid);

        assertThat(dto.productId()).isEqualTo(pid);
        assertThat(dto.sku()).isEqualTo("SKU-001");
        assertThat(dto.price()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        assertThat(dto.stockQuantity()).isEqualTo(50);
    }

    @Test
    void getProduct_withoutStockHash_returnsDtoWithZeroStock() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("product:" + pid, "stockQuantity")).thenReturn(null);
        when(hashOperations.get("product:" + pid, "productId")).thenReturn(pid.toString());
        when(hashOperations.get("product:" + pid, "sku")).thenReturn("SKU-001");
        when(hashOperations.get("product:" + pid, "price")).thenReturn("49.99");

        ProductCacheDto dto = service.getProduct(pid);

        assertThat(dto.stockQuantity()).isEqualTo(0);
    }

    @Test
    void getStock_keyMissing_throwsProductNotFoundException() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(false);

        assertThatThrownBy(() -> service.getStock(pid))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getStock_missingHashValue_returnsZero() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("product:" + pid, "stockQuantity")).thenReturn(null);

        Integer stock = service.getStock(pid);

        assertThat(stock).isEqualTo(0);
    }

    @Test
    void getStock_existingHashValue_returnsParsedInteger() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.hasKey("product:" + pid)).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("product:" + pid, "stockQuantity")).thenReturn("17");

        Integer stock = service.getStock(pid);

        assertThat(stock).isEqualTo(17);
    }

    @Test
    void setStock_putsAllHashFields() {
        UUID pid = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        service.setStock(pid, "SKU-1", "9.99", "100");

        verify(hashOperations).put("product:" + pid, "productId", pid.toString());
        verify(hashOperations).put("product:" + pid, "sku", "SKU-1");
        verify(hashOperations).put("product:" + pid, "price", "9.99");
        verify(hashOperations).put("product:" + pid, "stockQuantity", "100");
    }
}
