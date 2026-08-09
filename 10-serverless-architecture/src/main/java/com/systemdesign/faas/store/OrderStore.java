package com.systemdesign.faas.store;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Stands in for a persistent backing store (e.g. DynamoDB) that real Lambda functions would use,
 * since execution environments are ephemeral and hold no state between cold starts. This Spring
 * singleton bean lives at the JVM-process level (NOT inside any function instance), which is what
 * lets {@code dailySalesReport} demonstrably read what {@code createOrder} wrote across many
 * separate, independently-cold-started invocations — exactly like a real Lambda reading from
 * DynamoDB rather than from in-memory state that would vanish between invocations.
 */
@Component
public class OrderStore {

    private static final long UNIT_PRICE = 1_000; // flat price per unit, demo only

    private final List<Order> orders = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();

    public Order addOrder(String customerId, List<OrderItem> items) {
        int id = counter.incrementAndGet();
        long total = items.stream().mapToLong(item -> (long) item.qty() * UNIT_PRICE).sum();
        Order order = new Order("order_" + id, customerId, List.copyOf(items), total, Instant.now());
        orders.add(order);
        return order;
    }

    public List<Order> listOrders() {
        return List.copyOf(orders);
    }

    public OrderSummary summarize() {
        long totalRevenue = orders.stream().mapToLong(Order::total).sum();
        return new OrderSummary(orders.size(), totalRevenue);
    }
}
