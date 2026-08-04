package com.systemdesign.asyncqueue.workers;

/** Direct port of src/workers/sleep.ts. */
public final class Sleep {

    private Sleep() {
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during simulated processing delay", e);
        }
    }
}
