package com.systemdesign.modularmonolith.ordering.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload published on {@code order.created}. Deliberately a flat, self-contained snapshot --
 * consumers (Inventory, Notifications) must not need to call back into Ordering or Catalog to act
 * on it. Mirrors {@code order-created.event.ts}.
 */
public record OrderCreatedEvent(UUID orderId, UUID userId, BigDecimal total, List<Item> items) {

    public record Item(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    }
}
