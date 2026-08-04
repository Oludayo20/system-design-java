package com.systemdesign.ecommarketplace.orders.repository;

import com.systemdesign.ecommarketplace.orders.entity.Order;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bound to the primary EntityManagerFactory via PrimaryDataSourceConfig's @EnableJpaRepositories. */
public interface OrderRepository extends JpaRepository<Order, UUID> {}
