package com.caorderapi.integration.repository;

import com.caorderapi.model.ProductEntity;
import com.caorderapi.repository.ProductRepository;
import com.caorderapi.service.IInventoryCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
@Transactional
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private IInventoryCacheService inventoryCacheService;

    @Test
    void decrementStockIfAvailable_whenSufficientStock_updatesRow() {
        ProductEntity product = saveProduct(10);

        int updated = productRepository.decrementStockIfAvailable(product.getId(), 4);

        ProductEntity reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void decrementStockIfAvailable_whenInsufficientStock_doesNotUpdate() {
        ProductEntity product = saveProduct(3);

        int updated = productRepository.decrementStockIfAvailable(product.getId(), 5);

        ProductEntity reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated).isEqualTo(0);
        assertThat(reloaded.getStockQuantity()).isEqualTo(3);
    }

    private ProductEntity saveProduct(int stock) {
        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID());
        product.setSku("SKU-PRD-" + UUID.randomUUID());
        product.setName("Repository Product");
        product.setDescription("Repository test seed");
        product.setPrice(BigDecimal.valueOf(19.99));
        product.setStockQuantity(stock);
        product.setActive(true);
        return productRepository.save(product);
    }
}

