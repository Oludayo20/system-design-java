package com.systemdesign.ecommarketplace.marketplace.repository;

import com.systemdesign.ecommarketplace.marketplace.entity.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Bound to the primary EntityManagerFactory via PrimaryDataSourceConfig's @EnableJpaRepositories. */
public interface ProductRepository extends JpaRepository<Product, UUID> {

  List<Product> findAllByOrderByNameAsc();

  /** Backs MarketplaceService.decrementStock, called by the Inventory worker. */
  @Modifying
  @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id")
  int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
