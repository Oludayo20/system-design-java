package com.systemdesign.ecommarketplace.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Mirrors CreateOrderItemDto in src/modules/orders/dto/create-order.dto.ts. */
public record CreateOrderItemRequest(@NotNull UUID productId, @Min(1) int quantity) {}
