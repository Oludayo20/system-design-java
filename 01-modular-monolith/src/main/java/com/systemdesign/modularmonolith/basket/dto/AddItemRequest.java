package com.systemdesign.modularmonolith.basket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Add a catalog product to the basket.")
public record AddItemRequest(
        @Schema(description = "Product UUID from GET /catalog/products") @NotNull UUID productId,
        @Schema(example = "1", minimum = "1") @Min(1) int quantity) {
}
