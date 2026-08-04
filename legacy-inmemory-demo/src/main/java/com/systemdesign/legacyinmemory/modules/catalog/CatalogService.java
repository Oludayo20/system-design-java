package com.systemdesign.legacyinmemory.modules.catalog;

import com.systemdesign.legacyinmemory.infrastructure.Delay;
import com.systemdesign.legacyinmemory.infrastructure.InMemoryCache;
import com.systemdesign.legacyinmemory.infrastructure.InMemoryDatabase;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Java port of {@code modules/catalog/catalog.module.js} - "Products, Categories, Search,
 * Product Images." Demonstrates the Redis-vs-PostgreSQL flow: "Instead of PostgreSQL -&gt;
 * Redis -&gt; Product Found -&gt; Return. Faster."
 *
 * <p>The original's {@code seed()} is called explicitly by {@code bootstrap.js} right after
 * the module is constructed ({@code await catalog.seed();}); here that one-time startup seed
 * is triggered by {@link PostConstruct}, which runs at the equivalent point in the Spring
 * bean lifecycle (once, before the application starts serving requests).
 */
@Service
public class CatalogService {

    private final InMemoryDatabase db;
    private final InMemoryCache cache;

    public CatalogService(InMemoryDatabase db, InMemoryCache cache) {
        this.db = db;
        this.cache = cache;
    }

    @PostConstruct
    public void seed() {
        db.insert("products", new Product(1, "Laptop", 1500));
        db.insert("products", new Product(2, "Phone", 800));
        db.insert("products", new Product(3, "Headphones", 150));
    }

    public Product getProduct(int id) {
        String cacheKey = "product:" + id;
        Optional<Product> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            System.out.println("[Catalog] cache HIT for product " + id + " (Redis) - PostgreSQL was not touched");
            return cached.get();
        }

        System.out.println("[Catalog] cache MISS for product " + id + " - querying PostgreSQL...");
        Delay.sleep(100); // simulate a heavier DB query
        Optional<Product> product = db.find("products", (Product p) -> p.getId() == id);
        product.ifPresent(p -> cache.set(cacheKey, p, 60_000));
        return product.orElse(null);
    }

    public List<Product> listProducts() {
        return db.findAll("products");
    }
}
