package com.systemdesign.modularmonolith.ordering.dto;

import java.util.UUID;

/** Mirrors the {@code PlaceOrderResult} interface in {@code ordering.service.ts}. */
public record PlaceOrderResult(boolean success, UUID orderId) {
}
