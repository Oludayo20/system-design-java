package com.systemdesign.legacyinmemory.modules.ordering;

import com.systemdesign.legacyinmemory.common.AppException;
import com.systemdesign.legacyinmemory.infrastructure.EventQueue;
import com.systemdesign.legacyinmemory.infrastructure.InMemoryDatabase;
import com.systemdesign.legacyinmemory.infrastructure.Topic;
import com.systemdesign.legacyinmemory.modules.basket.BasketService;
import com.systemdesign.legacyinmemory.modules.basket.Cart;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Java port of {@code modules/ordering/ordering.module.js} - "Place Order, Cancel Order, Order
 * History."
 *
 * <p>The key idea from doc.md, preserved here: instead of
 * {@code await createOrder(); await sendEmail();} (customer waits), we save the order,
 * <b>publish</b> events, and return immediately. {@link EventQueue#publish} and
 * {@link Topic#publish} both only enqueue/dispatch and return - the actual slow work (stock
 * reduction, email, analytics, fraud scan) happens on background worker/subscriber threads
 * registered by {@code inventory}, {@code notification}, and {@code analytics}, so
 * {@link #placeOrder(int)} genuinely returns to its caller without waiting on them, exactly
 * like the original.
 */
@Service
public class OrderingService {

    private final InMemoryDatabase db;
    private final BasketService basket;
    private final EventQueue<OrderCreatedJob> orderQueue;
    private final Topic<SaleEvent> saleTopic;
    private final AtomicInteger nextOrderId = new AtomicInteger(1);

    public OrderingService(InMemoryDatabase db, BasketService basket,
                            EventQueue<OrderCreatedJob> orderQueue, Topic<SaleEvent> saleTopic) {
        this.db = db;
        this.basket = basket;
        this.orderQueue = orderQueue;
        this.saleTopic = saleTopic;
    }

    public Order placeOrder(int userId) {
        Cart cart = basket.getCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new AppException(400, "Cart is empty");
        }
        double total = basket.getTotal(userId);

        long startedAt = System.currentTimeMillis();
        Order order = new Order(nextOrderId.getAndIncrement(), userId, cart.getItems(), total, "CREATED", startedAt);
        db.insert("orders", order);
        System.out.println("[Ordering] order #" + order.getId() + " saved to PostgreSQL (total: $" + total + ")");

        // RabbitMQ-style: a job that MUST happen exactly once per order (reduce stock, send receipt).
        orderQueue.publish(new OrderCreatedJob(order.getId(), userId, order.getItems()));
        // Kafka-style: an event that MANY independent services care about (analytics, fraud checks, ...).
        saleTopic.publish(new SaleEvent(order.getId(), userId, total));

        System.out.println("[Ordering] published \"OrderCreated\" and returning to the customer after "
                + (System.currentTimeMillis() - startedAt) + "ms - NOT waiting for email/inventory/analytics");
        return order;
    }
}
