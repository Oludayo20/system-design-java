package com.systemdesign.faas.runtime;

/**
 * @param warmTtlMs how long an idle instance stays warm before the sweeper evicts it
 * @param coldStartLatencyMs real, measured extra latency injected on every cold start
 * @param sweepIntervalMs how often the background sweeper checks for idle instances to evict
 */
public record ExecutionEnvironmentManagerOptions(long warmTtlMs, long coldStartLatencyMs, long sweepIntervalMs) {

    public ExecutionEnvironmentManagerOptions {
        if (warmTtlMs <= 0) {
            throw new IllegalArgumentException("warmTtlMs must be positive");
        }
        if (coldStartLatencyMs < 0) {
            throw new IllegalArgumentException("coldStartLatencyMs must not be negative");
        }
        if (sweepIntervalMs <= 0) {
            throw new IllegalArgumentException("sweepIntervalMs must be positive");
        }
    }
}
