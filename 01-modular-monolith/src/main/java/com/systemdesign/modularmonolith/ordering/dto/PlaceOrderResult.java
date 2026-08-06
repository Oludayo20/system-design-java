package com.systemdesign.modularmonolith.ordering.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Immediate response after placing an order. Workers process asynchronously.")
public record PlaceOrderResult(
        @Schema(example = "true") boolean success,
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID orderId) {
}
