package com.systemdesign.modularmonolith.basket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/basket/entities/cart-item.entity.ts} ({@code basket.cart_items}).
 *
 * <p>{@code productId} is foreign-key-in-spirit only: deliberately no JPA relation and no DB
 * foreign key into {@code catalog.products}. Basket knows a product exists only by asking
 * {@code CatalogService#getProductForOrder(productId)} -- see {@link
 * com.systemdesign.modularmonolith.basket.BasketService}.</p>
 */
@Entity
@Table(name = "cart_items", schema = "basket",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_items_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
