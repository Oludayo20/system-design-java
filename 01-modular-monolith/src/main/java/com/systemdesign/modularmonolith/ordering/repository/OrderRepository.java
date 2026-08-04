package com.systemdesign.modularmonolith.ordering.repository;

import com.systemdesign.modularmonolith.ordering.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Explicit {@code JOIN FETCH} so {@code items} is initialized before the entity leaves the
     * transactional service method (mirrors {@code relations: ['items']} in
     * {@code ordering.service.ts#getOrder}); {@code spring.jpa.open-in-view} is disabled, so a
     * plain lazy collection would blow up when Jackson serializes the response.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItemsOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
