package com.systemdesign.modularmonolith.infrastructure.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The only way modules are allowed to talk to RabbitMQ. Publishing goes through here so every
 * event gets the same envelope shape and the same log line, and so no module needs to know the
 * exchange name or the underlying AMQP client.
 *
 * Mirrors {@code src/infrastructure/rabbitmq/event-bus.service.ts}. {@link #publish} returns a
 * {@link CompletableFuture} and is meant to be used fire-and-forget by callers -- the actual send
 * happens on a virtual thread (see {@code RabbitMqConfig#eventBusExecutor}) so it never blocks
 * the calling thread, matching the non-awaited
 * {@code void this.eventBus.publish(...).catch(...)} pattern in {@code ordering.service.ts}.
 */
@Slf4j
@Component
public class EventBus {

    private final RabbitTemplate rabbitTemplate;
    private final Executor executor;

    public EventBus(RabbitTemplate rabbitTemplate, Executor eventBusExecutor) {
        this.rabbitTemplate = rabbitTemplate;
        this.executor = eventBusExecutor;
    }

    public <T> CompletableFuture<Void> publish(String routingKey, T payload) {
        EventEnvelope<T> envelope = new EventEnvelope<>(routingKey, Instant.now().toString(), payload);
        log.info("Publishing \"{}\" -> {}", routingKey, RabbitMqConstants.DOMAIN_EVENTS_EXCHANGE);
        return CompletableFuture.runAsync(
                () -> rabbitTemplate.convertAndSend(RabbitMqConstants.DOMAIN_EVENTS_EXCHANGE, routingKey, envelope),
                executor);
    }
}
