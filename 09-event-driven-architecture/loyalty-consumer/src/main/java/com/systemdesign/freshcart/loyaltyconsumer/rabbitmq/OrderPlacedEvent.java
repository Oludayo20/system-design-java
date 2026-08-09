package com.systemdesign.freshcart.loyaltyconsumer.rabbitmq;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * loyalty-consumer's own copy of the envelope order-api publishes — deliberately duplicated
 * rather than shared, same as every other app in this project. {@code eventId} is what
 * {@code PointsService}'s idempotency check keys off.
 */
public record OrderPlacedEvent(UUID eventId, String eventType, Instant occurredAt, Payload payload) {

    public record Payload(UUID orderId, String customerId, List<Item> items, BigDecimal totalAmount) {
    }

    public record Item(String sku, String name, int quantity, BigDecimal unitPrice) {
    }
}
