package com.systemdesign.freshcart.orderapi.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Explicit {@code JOIN FETCH} so {@code items} is initialized before the entity leaves the
     * transactional method — {@code spring.jpa.open-in-view} is disabled, so a plain lazy
     * collection would blow up when Jackson serializes the response.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);
}
