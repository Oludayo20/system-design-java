package com.systemdesign.legacyinmemory.modules.notification;

import com.systemdesign.legacyinmemory.infrastructure.Delay;
import com.systemdesign.legacyinmemory.infrastructure.EventQueue;
import com.systemdesign.legacyinmemory.modules.ordering.OrderCreatedJob;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Java port of {@code modules/notification/notification.worker.js} - doc.md: "Send Email"
 * (~5s in the doc's example, scaled down here for a fast demo) and "Send Push". Runs as a
 * RabbitMQ-style competing consumer.
 *
 * <p>See {@link com.systemdesign.legacyinmemory.modules.inventory.InventoryWorker}'s Javadoc
 * for the important caveat: this worker competes with the inventory worker on the same
 * {@code orderQueue}, so only one of the two actually runs per order.
 */
@Component
public class NotificationWorker {

    private final EventQueue<OrderCreatedJob> orderQueue;

    public NotificationWorker(EventQueue<OrderCreatedJob> orderQueue) {
        this.orderQueue = orderQueue;
    }

    @PostConstruct
    public void register() {
        orderQueue.registerWorker("notification-worker", payload -> {
            Delay.sleep(700);
            System.out.println("  [Notification Worker] sent receipt email + push notification for order #"
                    + payload.orderId());
        });
    }
}
