package com.systemdesign.modularmonolith.catalog;

import com.systemdesign.modularmonolith.catalog.entity.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/catalog/catalog.controller.ts}. Public (no auth required -- see
 * {@code identity.security.SecurityConfig}).
 */
@RestController
@RequestMapping("/catalog")
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
    public Product getProduct(@PathVariable UUID id) {
        return catalogService.getProduct(id);
    }
}
