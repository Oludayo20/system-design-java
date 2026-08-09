package com.systemdesign.faas.runtime;

/**
 * The outcome of a single {@code ExecutionEnvironmentManager.invoke(...)} call — everything a
 * trigger adapter needs to prove cold vs. warm and report billing.
 */
public record InvokeResult(
        String functionName,
        String requestId,
        boolean cold,
        long durationMs,
        long billedMs,
        String instanceId,
        LambdaResponse response) {
}
