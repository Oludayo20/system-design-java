package com.systemdesign.modularmonolith.basket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Current basket with resolved product prices.")
public record BasketView(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID userId,
        List<BasketLine> items,
        @Schema(example = "1899.00") BigDecimal total) {
}
