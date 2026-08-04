package com.systemdesign.modularmonolith.catalog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/catalog/entities/product.entity.ts} ({@code catalog.products}).
 *
 * <p>{@code categoryId} is a plain column and {@code category} is a separate, read-only, lazy
 * relation mapped to the same join column (TypeORM's entity does the same dual mapping) --
 * {@code category} is {@code @JsonIgnore}d so it never triggers lazy loading during JSON
 * serialization; API responses only ever exposed {@code categoryId} in the source (a bare
 * {@code find()}/{@code findOne()} never loads the relation, so it was `undefined` and omitted
 * from the NestJS JSON responses too).</p>
 *
 * <p>{@code price} is a Java {@link BigDecimal} and serializes as a JSON number (e.g.
 * {@code 1899.00}); the NestJS source returns it as a string because node-postgres returns
 * {@code numeric} columns as strings. This is a deliberate, idiomatic deviation -- callers should
 * treat the field as numeric either way.</p>
 */
@Entity
@Table(name = "products", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "category_id")
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    @JsonIgnore
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
