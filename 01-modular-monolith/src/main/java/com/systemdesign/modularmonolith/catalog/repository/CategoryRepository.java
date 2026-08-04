package com.systemdesign.modularmonolith.catalog.repository;

import com.systemdesign.modularmonolith.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Registered alongside Product (mirrors {@code TypeOrmModule.forFeature([Product, Category])}
 * in catalog.module.ts) but, as in the source, not currently injected anywhere -- there is no
 * standalone categories endpoint. */
public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
