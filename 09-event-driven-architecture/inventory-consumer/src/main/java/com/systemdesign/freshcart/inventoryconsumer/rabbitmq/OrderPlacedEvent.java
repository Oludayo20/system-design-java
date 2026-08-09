package com.systemdesign.freshcart.inventoryconsumer.rabbitmq;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * inventory-consumer's own copy of the envelope order-api publishes. This record is deliberately
 * duplicated (not shared via a common library) in every app in this project — the only thing
 * tying these five independent Maven modules together is an agreement on the exchange name,
 * routing key, and this JSON shape, documented in the README. Spring AMQP's
 * {@code Jackson2JsonMessageConverter} defaults to {@code TypePrecedence.INFERRED}, so it
 * deserializes into this local class based on the {@code @RabbitListener} method's parameter
 * type rather than the producer's {@code __TypeId__} header (which names order-api's own,
 * differently-packaged, class) — exactly what's needed for independently-deployed consumers that
 * share no code.
 */
public record OrderPlacedEvent(UUID eventId, String eventType, Instant occurredAt, Payload payload) {

    public record Payload(UUID orderId, String customerId, List<Item> items, BigDecimal totalAmount) {
    }

    public record Item(String sku, String name, int quantity, BigDecimal unitPrice) {
    }
}
