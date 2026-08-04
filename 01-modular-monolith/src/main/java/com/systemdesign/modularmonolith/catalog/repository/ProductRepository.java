package com.systemdesign.modularmonolith.catalog.repository;

import com.systemdesign.modularmonolith.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByOrderByCreatedAtDesc();

    /**
     * Conditional decrement (only applies if there's enough stock), mirroring the
     * {@code createQueryBuilder().update(Product)...where('id = :id AND stock >= :quantity')}
     * call in {@code catalog.service.ts#decrementStock}. Returns the number of rows updated (0 or
     * 1) so the caller can tell whether it actually happened.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
