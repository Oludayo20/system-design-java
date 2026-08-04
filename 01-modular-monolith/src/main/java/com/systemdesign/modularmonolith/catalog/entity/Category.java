package com.systemdesign.modularmonolith.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Mirrors {@code src/modules/catalog/entities/category.entity.ts} ({@code catalog.categories}).
 * No endpoint returns Category on its own in the source project (only products are exposed over
 * HTTP), so the reverse {@code products} relation from the TypeORM entity is intentionally not
 * reproduced here -- nothing in the API surface depends on it.
 */
@Entity
@Table(name = "categories", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 140, unique = true)
    private String slug;
}
