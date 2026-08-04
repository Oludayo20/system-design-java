package com.systemdesign.asyncqueue.rabbitmq;

import java.util.Map;

/**
 * Pure decision functions driving the retry-vs-dead-letter choice, kept dependency-free (no
 * channel, no broker) so they are unit-testable without a live RabbitMQ connection — a direct
 * port of src/common/rabbitmq/retry.util.ts.
 */
public final class RetryUtil {

    private RetryUtil() {
    }

    /**
     * Given how many delivery attempts a job has now had, should it be routed to the dead letter
     * queue instead of retried again?
     */
    public static boolean shouldDeadLetter(int attempt, int maxAttempts) {
        return attempt >= maxAttempts;
    }

    /** Defaults to the topology-wide {@link Topology#MAX_DELIVERY_ATTEMPTS} when no max is given. */
    public static boolean shouldDeadLetter(int attempt) {
        return shouldDeadLetter(attempt, Topology.MAX_DELIVERY_ATTEMPTS);
    }

    /**
     * The retry count travels as a message header (rather than, say, encoded into the payload)
     * because it's transport metadata, not domain data — this way worker handlers never need to
     * know or unwrap it, and the same header shape works for every queue in the topology.
     */
    public static int nextAttempt(Map<String, Object> headers) {
        Object current = headers == null ? null : headers.get(Topology.RETRY_COUNT_HEADER);
        int currentValue = (current instanceof Number number) ? number.intValue() : 0;
        return currentValue + 1;
    }
}
