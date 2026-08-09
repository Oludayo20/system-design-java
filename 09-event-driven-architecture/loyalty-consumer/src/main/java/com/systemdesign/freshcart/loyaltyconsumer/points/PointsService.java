package com.systemdesign.freshcart.loyaltyconsumer.points;

import com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency, for real.
 *
 * <p>RabbitMQ (like every broker offering at-least-once delivery) can redeliver a message that
 * was already successfully processed — e.g. the consumer crashes after doing the work but before
 * it manages to ack, or a network blip causes the broker to assume the worst and redeliver. "Only
 * award points once per order" therefore can't rely on "only receive the message once", because
 * that guarantee doesn't exist. Instead we track which {@code eventId}s have already been applied
 * (a {@link Set}, in-memory for this demo; a real system would use a unique constraint on
 * {@code event_id} in whatever table records the side effect) and skip anything already seen.
 *
 * <p>See {@code scripts.SimulateDuplicateDeliveryApp} for a program that sends the exact same
 * eventId to this consumer's queue twice, and the README for how to run it and read the result.
 */
@Slf4j
@Service
public class PointsService {

    private final Map<String, Integer> pointsByCustomer = new ConcurrentHashMap<>();
    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();

    public synchronized void awardForOrder(OrderPlacedEvent event) {
        if (processedEventIds.contains(event.eventId())) {
            log.warn("Duplicate delivery of eventId={} (orderId={}) — already processed. "
                    + "Skipping so points are not awarded twice.",
                    event.eventId(), event.payload().orderId());
            return;
        }

        int points = Math.max(1, event.payload().totalAmount().setScale(0, RoundingMode.HALF_UP).intValue());
        String customerId = event.payload().customerId();
        int next = pointsByCustomer.getOrDefault(customerId, 0) + points;
        pointsByCustomer.put(customerId, next);
        processedEventIds.add(event.eventId());

        log.info("order.placed (orderId={}, eventId={}): awarded {} points to {} (total now {})",
                event.payload().orderId(), event.eventId(), points, customerId, next);
    }

    public synchronized PointsSummary getSummary() {
        List<CustomerPoints> customers = pointsByCustomer.entrySet().stream()
                .map(entry -> new CustomerPoints(entry.getKey(), entry.getValue()))
                .toList();
        return new PointsSummary(customers, processedEventIds.size());
    }
}
