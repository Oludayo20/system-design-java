package com.systemdesign.freshcart.notificationconsumer.rabbitmq;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * notification-consumer's own copy of the envelope order-api publishes — deliberately duplicated
 * rather than shared, same as every other app in this project. See the README for the JSON shape
 * every consumer agrees on.
 */
public record OrderPlacedEvent(UUID eventId, String eventType, Instant occurredAt, Payload payload) {

    public record Payload(UUID orderId, String customerId, List<Item> items, BigDecimal totalAmount) {
    }

    public record Item(String sku, String name, int quantity, BigDecimal unitPrice) {
    }
}
