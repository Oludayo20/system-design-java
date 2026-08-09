package com.systemdesign.bookhive.catalog.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveStockRequest(
        @Schema(example = "1", description = "Units to reserve (decrement from stock)")
        @NotNull @Min(1) Integer quantity) {
}
