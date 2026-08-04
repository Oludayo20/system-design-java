package com.systemdesign.resilience.resilience;

import java.util.function.Supplier;

public final class RetryUtil {

    private RetryUtil() {}

    public record RetryOptions(int maxAttempts, long delayMs) {}

    public static <T> T withRetries(Supplier<T> fn, RetryOptions options) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= options.maxAttempts(); attempt++) {
            try {
                return fn.get();
            } catch (RuntimeException error) {
                lastError = error;
                if (attempt < options.maxAttempts()) {
                    sleep(options.delayMs());
                }
            }
        }

        throw lastError;
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", interrupted);
        }
    }
}
