package com.systemdesign.ecommarketplace.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.systemdesign.ecommarketplace.common.exceptions.NotFoundException;
import com.systemdesign.ecommarketplace.infrastructure.redis.RedisService;
import com.systemdesign.ecommarketplace.marketplace.dto.ProductForOrder;
import com.systemdesign.ecommarketplace.marketplace.entity.Category;
import com.systemdesign.ecommarketplace.marketplace.entity.Product;
import com.systemdesign.ecommarketplace.marketplace.repository.CategoryRepository;
import com.systemdesign.ecommarketplace.marketplace.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mirrors src/modules/marketplace/marketplace.service.ts. */
@Service
public class MarketplaceService {

  private static final Logger log = LoggerFactory.getLogger(MarketplaceService.class);

  private static final String PRODUCT_LIST_CACHE_KEY = "marketplace:products:v1";
  private static final long PRODUCT_LIST_TTL_SECONDS = 60;

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final RedisService redisService;

  public MarketplaceService(
      ProductRepository productRepository, CategoryRepository categoryRepository, RedisService redisService) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.redisService = redisService;
  }

  /**
   * Cache-aside: check Redis first, fall through to Postgres on a miss and
   * repopulate the cache. This is doc.md's "/products -> Redis -> Product
   * Found -> Return" path. Mutations (order placement decrementing stock)
   * invalidate this key rather than trying to patch it in place.
   */
  public List<Product> listProducts() {
    var cached = redisService.get(PRODUCT_LIST_CACHE_KEY, new TypeReference<List<Product>>() {});
    if (cached.isPresent()) {
      log.debug("marketplace:products cache hit");
      return cached.get();
    }

    log.debug("marketplace:products cache miss - querying Postgres");
    List<Product> products = productRepository.findAllByOrderByNameAsc();
    redisService.set(PRODUCT_LIST_CACHE_KEY, products, PRODUCT_LIST_TTL_SECONDS);
    return products;
  }

  public Product getProductById(UUID id) {
    return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
  }

  public List<Category> listCategories() {
    return categoryRepository.findAllByOrderByNameAsc();
  }

  /**
   * Narrow, read-only method for the Order module to validate a purchase
   * against - see ProductForOrder's doc comment for the module-boundary
   * rationale.
   */
  public ProductForOrder getProductForOrder(UUID productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
    return new ProductForOrder(product.getId(), product.getName(), product.getPriceCents(), product.getStock());
  }

  /** Called by the Inventory worker in reaction to order.created. */
  @Transactional("primaryTransactionManager")
  public void decrementStock(UUID productId, int quantity) {
    productRepository.decrementStock(productId, quantity);
    redisService.delete(PRODUCT_LIST_CACHE_KEY);
  }
}
