package com.systemdesign.ecommarketplace.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Mirrors src/modules/marketplace/entities/product.entity.ts. Lives on the PRIMARY database. */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "price_cents", nullable = false)
  private int priceCents;

  @Column(nullable = false)
  private int stock;

  @Column(name = "image_url")
  private String imageUrl;

  // The FK column itself, writable - kept separate from the `category`
  // relation below (which is read-only/insertable=false,updatable=false)
  // because both map to the same physical "category_id" column, mirroring
  // TypeORM's dual @Column + @JoinColumn mapping onto one column.
  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id", insertable = false, updatable = false)
  private Category category;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
