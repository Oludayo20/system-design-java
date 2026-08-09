package com.systemdesign.freshcart.orderapi.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemDto(

        @Schema(example = "milk-1l")
        @NotBlank
        String sku,

        @Schema(example = "Whole Milk 1L")
        @NotBlank
        String name,

        @Schema(example = "2")
        @Positive
        int quantity,

        @Schema(example = "1.50", description = "Unit price in the local currency")
        @Positive
        BigDecimal unitPrice) {
}
