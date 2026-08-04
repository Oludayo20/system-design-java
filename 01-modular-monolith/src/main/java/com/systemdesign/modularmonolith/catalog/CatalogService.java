package com.systemdesign.modularmonolith.catalog;

import com.systemdesign.modularmonolith.catalog.dto.ProductForOrder;
import com.systemdesign.modularmonolith.catalog.entity.Product;
import com.systemdesign.modularmonolith.catalog.repository.ProductRepository;
import com.systemdesign.modularmonolith.infrastructure.redis.RedisService;
import com.systemdesign.modularmonolith.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/catalog/catalog.service.ts}. This is the ONLY public surface of the
 * Catalog module -- other modules (Basket, Ordering, Inventory) may call
 * {@link #getProductForOrder} / {@link #decrementStock}, but nothing outside this package injects
 * {@link ProductRepository} directly. (Enforced here by convention/package layout, the same way
 * the NestJS source enforces it by simply not exporting the repository from
 * {@code catalog.module.ts} -- Spring's ApplicationContext doesn't have a hard module-visibility
 * concept the way Nest's DI container does.)
 */
@Slf4j
@Service
public class CatalogService {

    private static final long PRODUCT_CACHE_TTL_SECONDS = 300;

    private final ProductRepository products;
    private final RedisService redisService;

    public CatalogService(ProductRepository products, RedisService redisService) {
        this.products = products;
        this.redisService = redisService;
    }

    public List<Product> listProducts() {
        return products.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Cache-aside on the hot product-read path: check Redis first, fall back to Postgres on a
     * miss, then populate the cache with a TTL so the next read is served without hitting the DB.
     */
    public Product getProduct(UUID id) {
        String cacheKey = productCacheKey(id);
        Product cached = redisService.getJson(cacheKey, Product.class);
        if (cached != null) {
            log.debug("Cache hit for product {}", id);
            return cached;
        }

        log.debug("Cache miss for product {}, reading Postgres", id);
        Product product = products.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

        redisService.setJson(cacheKey, product, PRODUCT_CACHE_TTL_SECONDS);
        return product;
    }

    /**
     * Narrow, read-only interface for other modules (Basket, Ordering). Deliberately returns only
     * what a caller needs to price a cart / snapshot an order line item, never the full entity.
     */
    public ProductForOrder getProductForOrder(UUID id) {
        Product product = getProduct(id);
        return new ProductForOrder(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }

    /**
     * Called only by the Inventory consumer after {@code order.created}. Bypasses the cache-aside
     * read path on purpose (this is a write) and invalidates the cached copy so the next read
     * reflects the new stock level instead of serving a stale cached value.
     */
    @Transactional
    public void decrementStock(UUID id, int quantity) {
        int affected = products.decrementStock(id, quantity);
        if (affected == 0) {
            log.warn("Could not decrement stock for product {} by {} (insufficient stock?)", id, quantity);
            return;
        }

        redisService.del(productCacheKey(id));
        log.info("Decremented stock for product {} by {}", id, quantity);
    }

    private static String productCacheKey(UUID id) {
        return "catalog:product:" + id;
    }
}
