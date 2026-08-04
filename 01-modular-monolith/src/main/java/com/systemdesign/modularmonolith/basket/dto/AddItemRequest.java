package com.systemdesign.modularmonolith.basket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Mirrors {@code add-item.dto.ts}. */
public record AddItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity
) {
}
