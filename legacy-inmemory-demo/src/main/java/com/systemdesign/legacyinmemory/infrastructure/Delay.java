package com.systemdesign.legacyinmemory.infrastructure;

/**
 * Java port of {@code infrastructure/delay.js}.
 *
 * <p>The original is a one-liner: {@code delay(ms)} returns a Promise that resolves after
 * {@code ms} milliseconds, and every "slow" operation in the demo (a simulated DB round-trip,
 * a simulated email send, a simulated retry backoff, ...) does {@code await delay(ms)}.
 *
 * <p>Node is single-threaded, so awaiting a timer never blocks anything else in the process.
 * In this Java port, every caller of {@link #sleep(long)} already runs on its own thread
 * (a servlet request-handling thread, or a dedicated queue-worker/topic-subscriber thread -
 * see {@link EventQueue} and {@link Topic}), so a plain blocking {@link Thread#sleep(long)}
 * reproduces the same "this particular unit of work pauses, nothing else does" behavior
 * without needing a reactive/async rewrite of the whole call stack.
 */
public final class Delay {

    private Delay() {
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during simulated delay", e);
        }
    }
}
