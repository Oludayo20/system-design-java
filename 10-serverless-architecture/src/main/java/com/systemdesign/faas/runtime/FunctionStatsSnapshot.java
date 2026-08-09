package com.systemdesign.faas.runtime;

/**
 * Point-in-time, read-only view of a function's accumulated stats — what {@code GET
 * /_runtime/stats} returns per function name.
 *
 * @param warmInstances idle, still-warm instances right now (0 means "scaled to zero")
 */
public record FunctionStatsSnapshot(
        long invocations,
        long coldStarts,
        long warmStarts,
        long totalBilledMs,
        int warmInstances) {
}
