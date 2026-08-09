package com.systemdesign.freshcart.orderapi.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Every event on {@code grocery_events} carries this envelope. {@code eventId} is what makes
 * idempotency possible downstream (see loyalty-consumer) — it must be stable across redeliveries
 * of "the same" event, so it is generated once, at the moment the event is created, never
 * regenerated on retry/replay.
 *
 * This record is intentionally duplicated (not shared via a common library) in every app in this
 * project — order-api, inventory-consumer, notification-consumer, analytics-consumer, and
 * loyalty-consumer each define their own copy. That's deliberate: the only thing tying these five
 * independent Maven modules together is an agreement on the exchange name, routing key, and this
 * JSON shape — documented in the README, not enforced by a shared package. That is what "loose
 * coupling" looks like in practice, not just in theory.
 */
public record OrderPlacedEvent(UUID eventId, String eventType, Instant occurredAt, Payload payload) {

    public record Payload(UUID orderId, String customerId, List<Item> items, BigDecimal totalAmount) {
    }

    public record Item(String sku, String name, int quantity, BigDecimal unitPrice) {
    }
}
