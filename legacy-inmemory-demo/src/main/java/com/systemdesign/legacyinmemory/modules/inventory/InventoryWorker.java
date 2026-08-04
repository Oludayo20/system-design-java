package com.systemdesign.legacyinmemory.modules.inventory;

import com.systemdesign.legacyinmemory.infrastructure.Delay;
import com.systemdesign.legacyinmemory.infrastructure.EventQueue;
import com.systemdesign.legacyinmemory.modules.ordering.OrderCreatedJob;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Java port of {@code modules/inventory/inventory.worker.js} - doc.md: "Inventory Module -&gt;
 * Reduce Stock", a RabbitMQ-style competing consumer on the order queue.
 *
 * <p><b>Note on the original's behavior, preserved here:</b> this worker is registered on the
 * same shared {@code orderQueue} as {@link com.systemdesign.legacyinmemory.modules.notification.NotificationWorker}
 * (see {@code bootstrap.js}, which calls both {@code registerInventoryWorker(orderQueue)} and
 * {@code registerNotificationWorker(orderQueue)} on one {@code Queue}). Because {@code Queue}
 * is a competing-consumer queue where each job is handled exactly once, for any single order
 * only ONE of {inventory, notification} actually processes that order's job - whichever
 * worker's polling loop grabs it first - not both. This looks like it may be an oversight in
 * the original (you'd normally want both a stock-reduction *and* a receipt-email job per
 * order), but per the porting brief this is a faithful reproduction of the original's actual
 * runtime behavior, not a "fixed" version of it.
 */
@Component
public class InventoryWorker {

    private final EventQueue<OrderCreatedJob> orderQueue;

    public InventoryWorker(EventQueue<OrderCreatedJob> orderQueue) {
        this.orderQueue = orderQueue;
    }

    @PostConstruct
    public void register() {
        orderQueue.registerWorker("inventory-worker", payload -> {
            Delay.sleep(300);
            System.out.println("  [Inventory Worker] reduced stock for order #" + payload.orderId());
        });
    }
}
