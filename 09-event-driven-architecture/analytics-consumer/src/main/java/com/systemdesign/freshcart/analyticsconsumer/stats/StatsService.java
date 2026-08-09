package com.systemdesign.freshcart.analyticsconsumer.stats;

import com.systemdesign.freshcart.analyticsconsumer.rabbitmq.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * In-memory running counters — good enough for a teaching demo of "analytics reacts
 * independently to order.placed." A production analytics-consumer would write to a proper
 * time-series/warehouse store instead.
 */
@Slf4j
@Service
public class StatsService {

    private int ordersToday = 0;
    private BigDecimal revenueToday = BigDecimal.ZERO;
    private UUID lastOrderId;
    private Instant lastUpdatedAt;

    public synchronized void recordOrder(OrderPlacedEvent event) {
        ordersToday += 1;
        revenueToday = revenueToday.add(event.payload().totalAmount());
        lastOrderId = event.payload().orderId();
        lastUpdatedAt = Instant.now();
        log.info("order.placed (orderId={}, eventId={}): ordersToday={}, revenueToday={}",
                event.payload().orderId(), event.eventId(), ordersToday, revenueToday);
    }

    public synchronized SalesStats getStats() {
        return new SalesStats(
                ordersToday,
                revenueToday.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                lastOrderId,
                lastUpdatedAt == null ? null : lastUpdatedAt.toString());
    }
}
