package com.systemdesign.modularmonolith.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The only shape of Catalog data other modules are allowed to see. Basket and Ordering depend on
 * this record (via {@code CatalogService#getProductForOrder}), never on the {@code Product}
 * entity or the {@code catalog.products} table directly. Mirrors {@code catalog.types.ts}.
 */
public record ProductForOrder(UUID id, String name, BigDecimal price, int stock) {
}
