package com.systemdesign.ecommarketplace.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order placed; workers process asynchronously.")
public record CreateOrderResult(
    @Schema(example = "true") boolean success,
    @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String orderId) {}
