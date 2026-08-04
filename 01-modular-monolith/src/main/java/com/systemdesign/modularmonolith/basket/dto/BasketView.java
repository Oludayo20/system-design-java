package com.systemdesign.modularmonolith.basket.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Mirrors the {@code BasketView} interface in {@code basket.types.ts}. */
public record BasketView(UUID userId, List<BasketLine> items, BigDecimal total) {
}
