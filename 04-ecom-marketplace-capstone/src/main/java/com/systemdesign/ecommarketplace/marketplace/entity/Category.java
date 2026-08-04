package com.systemdesign.ecommarketplace.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Mirrors src/modules/marketplace/entities/category.entity.ts. Lives on the PRIMARY database. */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  // Never serialized and never eagerly loaded: nothing in this API returns
  // a Category with its product list, and loading it would risk a
  // Category -> Product -> Category cycle in Jackson's default serializer
  // (TypeORM's original never eager-loads this inverse side either).
  @JsonIgnore
  @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
  private List<Product> products;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
