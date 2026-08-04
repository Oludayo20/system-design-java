package com.systemdesign.modularmonolith.basket.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Mirrors the {@code BasketLine} interface in {@code basket.types.ts}. */
public record BasketLine(UUID productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
