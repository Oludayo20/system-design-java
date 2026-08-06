package com.systemdesign.ecommarketplace.marketplace;

import com.systemdesign.ecommarketplace.marketplace.entity.Category;
import com.systemdesign.ecommarketplace.marketplace.entity.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "marketplace", description = "Products and categories on primary DB")
@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

  private final MarketplaceService marketplaceService;

  public MarketplaceController(MarketplaceService marketplaceService) {
    this.marketplaceService = marketplaceService;
  }

  @GetMapping("/products")
  @Operation(summary = "List all products", description = "Redis cache-aside on product list.")
  @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
  public List<Product> listProducts() {
    return marketplaceService.listProducts();
  }

  @GetMapping("/products/{id}")
  @Operation(summary = "Get a product by ID")
  @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Product.class)))
  @ApiResponse(responseCode = "404", description = "Product not found.")
  public Product getProduct(@Parameter(description = "Product UUID") @PathVariable UUID id) {
    return marketplaceService.getProductById(id);
  }

  @GetMapping("/categories")
  @Operation(summary = "List all categories")
  @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Category.class))))
  public List<Category> listCategories() {
    return marketplaceService.listCategories();
  }
}
