package com.systemdesign.modularmonolith.ordering.dto;

import java.util.UUID;

/** Mirrors {@code OrderCancelledEvent} in {@code order-created.event.ts}. */
public record OrderCancelledEvent(UUID orderId, UUID userId) {
}
