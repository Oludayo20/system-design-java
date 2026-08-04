package com.systemdesign.legacyinmemory.modules.ordering;

/**
 * Payload published on the Kafka-style {@code saleTopic} - matches the original's
 * {@code { type: 'OrderCreated', orderId, userId, total }} event body. This is an event that
 * MANY independent services care about (analytics, fraud checks, ...) - see
 * {@link com.systemdesign.legacyinmemory.infrastructure.Topic}.
 */
public record SaleEvent(int orderId, int userId, double total) {
}
