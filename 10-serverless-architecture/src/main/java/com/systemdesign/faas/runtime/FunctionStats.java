package com.systemdesign.faas.runtime;

import java.util.concurrent.atomic.LongAdder;

/**
 * Mutable, thread-safe per-function counters accumulated across every {@code invoke(...)} call.
 * Package-private — exposed to callers only as an immutable {@link FunctionStatsSnapshot}.
 */
final class FunctionStats {

    private final LongAdder invocations = new LongAdder();
    private final LongAdder coldStarts = new LongAdder();
    private final LongAdder warmStarts = new LongAdder();
    private final LongAdder totalBilledMs = new LongAdder();

    void record(boolean cold, long billedMs) {
        invocations.increment();
        if (cold) {
            coldStarts.increment();
        } else {
            warmStarts.increment();
        }
        totalBilledMs.add(billedMs);
    }

    long invocations() {
        return invocations.sum();
    }

    long coldStarts() {
        return coldStarts.sum();
    }

    long warmStarts() {
        return warmStarts.sum();
    }

    long totalBilledMs() {
        return totalBilledMs.sum();
    }
}
