package com.systemdesign.resilience.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CircuitBreakerTest {

    @Test
    void opensAfterConsecutiveFailuresReachTheThreshold() {
        CircuitBreaker breaker = new CircuitBreaker(new CircuitBreakerOptions(3, 5_000));

        for (int i = 0; i < 3; i++) {
            assertThrows(IllegalStateException.class, () -> breaker.execute(() -> {
                throw new IllegalStateException("down");
            }));
        }

        assertEquals(CircuitState.OPEN, breaker.getState());

        assertThrows(IllegalStateException.class, () -> breaker.execute(() -> "ok"));
    }

    @Test
    void closesAgainAfterSuccessfulProbeInHalfOpen() {
        CircuitBreaker breaker = new CircuitBreaker(new CircuitBreakerOptions(1, 0));

        assertThrows(IllegalStateException.class, () -> breaker.execute(() -> {
            throw new IllegalStateException("down");
        }));

        assertEquals(CircuitState.OPEN, breaker.getState());

        String result = breaker.execute(() -> "recovered");
        assertEquals("recovered", result);
        assertEquals(CircuitState.CLOSED, breaker.getState());
    }
}
