package com.systemdesign.asyncqueue.rabbitmq;

/** A job handler that is allowed to throw — any exception routes the message to retry/DLQ. */
@FunctionalInterface
public interface ThrowingHandler<T> {
    void handle(T payload) throws Exception;
}
