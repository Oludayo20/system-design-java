package com.systemdesign.ecommarketplace.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Single order line item.")
public record CreateOrderItemRequest(
    @Schema(description = "Product UUID from GET /marketplace/products") @NotNull UUID productId,
    @Schema(example = "2", minimum = "1") @Min(1) int quantity) {}
