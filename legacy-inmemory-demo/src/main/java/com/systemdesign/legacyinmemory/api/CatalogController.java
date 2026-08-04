package com.systemdesign.legacyinmemory.api;

import com.systemdesign.legacyinmemory.modules.catalog.CatalogService;
import com.systemdesign.legacyinmemory.modules.catalog.Product;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Java port of the {@code /products*} routes in {@code api/server.js}. */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return catalogService.listProducts();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable int id) {
        Product product = catalogService.getProduct(id);
        if (product == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
        }
        return ResponseEntity.ok(product);
    }
}
