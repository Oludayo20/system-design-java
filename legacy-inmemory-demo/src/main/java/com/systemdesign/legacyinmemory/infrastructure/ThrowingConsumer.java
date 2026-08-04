package com.systemdesign.legacyinmemory.infrastructure;

/**
 * A {@link java.util.function.Consumer}-like functional interface that is allowed to throw,
 * mirroring how JS worker/subscriber handlers in {@code eventBus.js} are {@code async}
 * functions that may reject. Used for both {@link EventQueue} workers and {@link Topic}
 * subscribers.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T payload) throws Exception;
}
