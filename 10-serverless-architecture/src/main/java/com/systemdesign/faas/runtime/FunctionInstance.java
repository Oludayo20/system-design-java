package com.systemdesign.faas.runtime;

/**
 * One logical "execution environment": a constructed {@link LambdaFunction} instance plus enough
 * bookkeeping to decide whether it is still warm. Package-private — callers only ever see
 * {@link InvokeResult} and {@link FunctionStatsSnapshot}.
 */
final class FunctionInstance {

    private final String id;
    private final LambdaFunction handler;
    private final long createdAt;
    private volatile long lastUsedAt;

    FunctionInstance(String id, LambdaFunction handler, long now) {
        this.id = id;
        this.handler = handler;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    String id() {
        return id;
    }

    LambdaFunction handler() {
        return handler;
    }

    long createdAt() {
        return createdAt;
    }

    long lastUsedAt() {
        return lastUsedAt;
    }

    void touch(long now) {
        this.lastUsedAt = now;
    }
}
