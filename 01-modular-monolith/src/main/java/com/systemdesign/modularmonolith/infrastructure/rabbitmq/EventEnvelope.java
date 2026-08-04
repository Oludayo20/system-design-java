package com.systemdesign.modularmonolith.infrastructure.rabbitmq;

/**
 * Standard envelope wrapped around every payload published to the event bus, so every consumer
 * can log/trace/replay events uniformly regardless of which module emitted them.
 *
 * Mirrors the {@code EventEnvelope<T>} interface from {@code rabbitmq.constants.ts}.
 */
public record EventEnvelope<T>(String event, String timestamp, T payload) {
}
