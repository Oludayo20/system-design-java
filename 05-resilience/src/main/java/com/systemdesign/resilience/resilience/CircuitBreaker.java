package com.systemdesign.resilience.resilience;

import java.util.function.Supplier;

/**
 * Minimal circuit breaker: after N consecutive failures, short-circuit calls until the reset window
 * passes, then allow a single probe (HALF_OPEN).
 */
public class CircuitBreaker {

    private CircuitState state = CircuitState.CLOSED;
    private int consecutiveFailures = 0;
    private Long openedAt = null;

    private final CircuitBreakerOptions options;

    public CircuitBreaker(CircuitBreakerOptions options) {
        this.options = options;
    }

    public CircuitState getState() {
        refreshState();
        return state;
    }

    public <T> T execute(Supplier<T> fn) {
        refreshState();

        if (state == CircuitState.OPEN) {
            throw new IllegalStateException("Circuit breaker is OPEN");
        }

        try {
            T result = fn.get();
            onSuccess();
            return result;
        } catch (RuntimeException error) {
            onFailure();
            throw error;
        }
    }

    private void refreshState() {
        if (state != CircuitState.OPEN || openedAt == null) {
            return;
        }

        if (System.currentTimeMillis() - openedAt >= options.resetTimeoutMs()) {
            state = CircuitState.HALF_OPEN;
        }
    }

    private void onSuccess() {
        consecutiveFailures = 0;
        openedAt = null;
        state = CircuitState.CLOSED;
    }

    private void onFailure() {
        consecutiveFailures += 1;

        if (consecutiveFailures >= options.failureThreshold()) {
            state = CircuitState.OPEN;
            openedAt = System.currentTimeMillis();
        }
    }
}
