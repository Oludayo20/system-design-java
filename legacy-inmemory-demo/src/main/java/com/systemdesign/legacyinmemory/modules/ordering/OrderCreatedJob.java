package com.systemdesign.legacyinmemory.modules.ordering;

import com.systemdesign.legacyinmemory.modules.basket.CartItem;
import java.util.List;

/**
 * Payload published on the RabbitMQ-style {@code orderQueue} - matches the original's
 * {@code { type: 'OrderCreated', orderId, userId, items }} job body. This is a job that MUST
 * happen exactly once per order (reduce stock, send receipt) - see
 * {@link com.systemdesign.legacyinmemory.infrastructure.EventQueue}.
 */
public record OrderCreatedJob(int orderId, int userId, List<CartItem> items) {
}
