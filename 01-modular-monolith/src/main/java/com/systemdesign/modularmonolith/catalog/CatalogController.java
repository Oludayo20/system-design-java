package com.systemdesign.modularmonolith.catalog;

import com.systemdesign.modularmonolith.catalog.entity.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "catalog", description = "Products — public, no auth required")
@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    @Operation(summary = "List all products", description = "Returns every product in the catalog module.")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
    public List<Product> listProducts() {
        return catalogService.listProducts();
    }

    @GetMapping("/products/{id}")
    @Operation(
            summary = "Get a product by ID (cache-aside)",
            description = "Reads from Redis first; on cache miss loads from PostgreSQL and populates Redis with a TTL.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Product.class)))
    @ApiResponse(responseCode = "404", description = "No product with this ID.")
    public Product getProduct(
            @Parameter(description = "Product UUID") @PathVariable UUID id) {
        return catalogService.getProduct(id);
    }
}
