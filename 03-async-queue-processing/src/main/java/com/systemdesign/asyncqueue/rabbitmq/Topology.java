package com.systemdesign.asyncqueue.rabbitmq;

import java.util.List;

/**
 * The full RabbitMQ topology for the Uber-style ride.completed fan-out, including the
 * TTL + dead-letter-exchange retry mechanism. Declared idempotently (via {@link RabbitTopologyConfig})
 * by both the API (so publishing never targets a non-existent exchange) and the worker process —
 * a direct, constant-for-constant port of src/common/rabbitmq/topology.ts.
 *
 * <pre>
 *   producer                 ride_events (topic exchange)
 *     |  publish                    |  routing key "ride.completed"
 *     v                    +--------+--------+--------+
 *                          v                 v         v
 *                   email.queue     analytics.queue  loyalty.queue
 *                          |                 |         |
 *                    (worker fails)          |         |
 *                          v                 |         |
 *              email.queue.retry             |         |
 *              (x-message-ttl: 30s)          |         |
 *              (x-dead-letter-exchange: ride_events.dlx)
 *                          |
 *                 TTL expires, DLX redelivers
 *                 (routing key = "email.queue")
 *                          v
 *                   email.queue  &lt;---- ride_events.dlx (direct exchange)
 *                          |
 *                 still failing after MAX_DELIVERY_ATTEMPTS
 *                          v
 *              email.queue.dead-letter  (inspected manually)
 * </pre>
 */
public final class Topology {

    private Topology() {
    }

    public static final String RIDE_EVENTS_EXCHANGE = "ride_events";
    public static final String RIDE_EVENTS_DLX = "ride_events.dlx";
    public static final String RIDE_COMPLETED_ROUTING_KEY = "ride.completed";

    public static final long RETRY_TTL_MS = 30_000L;
    public static final int MAX_DELIVERY_ATTEMPTS = 3;
    public static final String RETRY_COUNT_HEADER = "x-retry-count";
    public static final String LAST_ERROR_HEADER = "x-last-error";

    /**
     * @param name           Durable queue a worker actually consumes from.
     * @param retryName      Holding queue: messages sit here for RETRY_TTL_MS, then get
     *                       dead-lettered back to {@code name}.
     * @param deadLetterName Terminal queue for messages that failed MAX_DELIVERY_ATTEMPTS times.
     *                       Never auto-drained.
     */
    public record WorkQueueDefinition(String name, String retryName, String deadLetterName) {
        static WorkQueueDefinition of(String name) {
            return new WorkQueueDefinition(name, name + ".retry", name + ".dead-letter");
        }
    }

    public static final WorkQueueDefinition EMAIL_QUEUE = WorkQueueDefinition.of("email.queue");
    public static final WorkQueueDefinition ANALYTICS_QUEUE = WorkQueueDefinition.of("analytics.queue");
    public static final WorkQueueDefinition LOYALTY_QUEUE = WorkQueueDefinition.of("loyalty.queue");

    public static final List<WorkQueueDefinition> ALL_WORK_QUEUES =
            List.of(EMAIL_QUEUE, ANALYTICS_QUEUE, LOYALTY_QUEUE);
}
