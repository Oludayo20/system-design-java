package com.systemdesign.bookhive.order.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @Schema(example = "3b1f8e2a-1c2d-4e3f-9a0b-123456789abc")
        @NotNull UUID bookId,

        @Schema(example = "1")
        @NotNull @Min(1) Integer quantity) {
}
