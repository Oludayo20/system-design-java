package com.systemdesign.modularmonolith.ordering;

import com.systemdesign.modularmonolith.basket.BasketService;
import com.systemdesign.modularmonolith.basket.dto.BasketLine;
import com.systemdesign.modularmonolith.basket.dto.BasketView;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.EventBus;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants;
import com.systemdesign.modularmonolith.ordering.dto.OrderCancelledEvent;
import com.systemdesign.modularmonolith.ordering.dto.OrderCreatedEvent;
import com.systemdesign.modularmonolith.ordering.dto.PlaceOrderResult;
import com.systemdesign.modularmonolith.ordering.entity.Order;
import com.systemdesign.modularmonolith.ordering.entity.OrderItem;
import com.systemdesign.modularmonolith.ordering.entity.OrderStatus;
import com.systemdesign.modularmonolith.ordering.repository.OrderRepository;
import com.systemdesign.modularmonolith.shared.exception.ForbiddenException;
import com.systemdesign.modularmonolith.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/ordering/ordering.service.ts}. Has zero dependency on Inventory or
 * Notifications -- it publishes {@code order.created} and moves on, unaware those modules exist.
 */
@Slf4j
@Service
public class OrderingService {

    private final OrderRepository orders;
    private final BasketService basketService;
    private final EventBus eventBus;

    public OrderingService(OrderRepository orders, BasketService basketService, EventBus eventBus) {
        this.orders = orders;
        this.basketService = basketService;
        this.eventBus = eventBus;
    }

    /**
     * Snapshot the basket into an order inside one DB transaction, commit it, and only THEN
     * publish {@code order.created}. Publishing before commit risks a consumer (Inventory) acting
     * on an order that a later rollback erases; publishing after commit means "the event exists"
     * is always a true statement about durable state. The event bus call itself is fire-and-forget
     * -- we never wait on Inventory/Notifications before responding to the customer.
     */
    @Transactional
    public PlaceOrderResult placeOrder(UUID userId) {
        BasketView basket = basketService.assertNotEmpty(userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setTotal(basket.total());
        for (BasketLine line : basket.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(line.productId());
            item.setProductName(line.name());
            item.setUnitPrice(line.unitPrice());
            item.setQuantity(line.quantity());
            item.setLineTotal(line.lineTotal());
            order.addItem(item);
        }

        order = orders.save(order);

        basketService.clear(userId);

        OrderCreatedEvent event = toOrderCreatedEvent(order);
        UUID orderId = order.getId();

        // Fire-and-forget: intentionally not awaited by the HTTP handler's response path. The
        // publish call itself resolves once RabbitMQ has accepted the message, not once
        // Inventory/Notifications have finished reacting to it.
        eventBus.publish(RabbitMqConstants.ORDER_CREATED, event)
                .exceptionally(ex -> {
                    log.error("Failed to publish order.created for {}", orderId, ex);
                    return null;
                });

        log.info("Order {} placed by user {}, total {}", order.getId(), userId, order.getTotal());
        return new PlaceOrderResult(true, order.getId());
    }

    public Order getOrder(UUID userId, UUID orderId) {
        Order order = orders.findByIdWithItems(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("This order does not belong to you");
        }
        return order;
    }

    public List<Order> listOrders(UUID userId) {
        return orders.findByUserIdWithItemsOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Order cancelOrder(UUID userId, UUID orderId) {
        Order order = getOrder(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orders.save(order);

        eventBus.publish(RabbitMqConstants.ORDER_CANCELLED, new OrderCancelledEvent(saved.getId(), userId))
                .exceptionally(ex -> {
                    log.error("Failed to publish order.cancelled for {}", saved.getId(), ex);
                    return null;
                });

        return saved;
    }

    private OrderCreatedEvent toOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderCreatedEvent.Item(
                        item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(order.getId(), order.getUserId(), order.getTotal(), items);
    }
}
