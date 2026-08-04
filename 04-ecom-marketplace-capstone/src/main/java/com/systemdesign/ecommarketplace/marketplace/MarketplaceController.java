package com.systemdesign.ecommarketplace.marketplace;

import com.systemdesign.ecommarketplace.marketplace.entity.Category;
import com.systemdesign.ecommarketplace.marketplace.entity.Product;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/marketplace/marketplace.controller.ts. Public - no JwtAuthGuard. */
@Tag(name = "marketplace")
@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

  private final MarketplaceService marketplaceService;

  public MarketplaceController(MarketplaceService marketplaceService) {
    this.marketplaceService = marketplaceService;
  }

  @GetMapping("/products")
  public List<Product> listProducts() {
    return marketplaceService.listProducts();
  }

  @GetMapping("/products/{id}")
  public Product getProduct(@PathVariable UUID id) {
    return marketplaceService.getProductById(id);
  }

  @GetMapping("/categories")
  public List<Category> listCategories() {
    return marketplaceService.listCategories();
  }
}
