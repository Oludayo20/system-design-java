package com.systemdesign.faas.runtime;

import java.time.Instant;

/**
 * Mirrors the metadata AWS Lambda passes alongside the event: which function, which invocation,
 * whether this execution ran in a freshly-constructed instance, and when.
 */
public record LambdaContext(String functionName, String requestId, boolean coldStart, Instant invokedAt) {
}
