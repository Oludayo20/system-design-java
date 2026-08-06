package com.systemdesign.modularmonolith.basket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Single line in the basket.")
public record BasketLine(
        UUID productId,
        @Schema(example = "Laptop Pro 15\"") String name,
        @Schema(example = "1899.00") BigDecimal unitPrice,
        @Schema(example = "1") int quantity,
        @Schema(example = "1899.00") BigDecimal lineTotal) {
}
