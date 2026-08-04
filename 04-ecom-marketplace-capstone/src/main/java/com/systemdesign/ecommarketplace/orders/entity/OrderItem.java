package com.systemdesign.ecommarketplace.orders.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors src/modules/orders/entities/order-item.entity.ts. Lives on the PRIMARY database. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "order_id", nullable = false, insertable = false, updatable = false, columnDefinition = "uuid")
  private UUID orderId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
  private UUID productId;

  // Snapshot at purchase time - Catalog prices can change after the sale.
  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price_cents", nullable = false)
  private int unitPriceCents;
}
