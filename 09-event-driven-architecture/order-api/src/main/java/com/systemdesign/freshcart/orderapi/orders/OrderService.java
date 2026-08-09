package com.systemdesign.freshcart.orderapi.orders;

import com.systemdesign.freshcart.orderapi.orders.dto.CreateOrderDto;
import com.systemdesign.freshcart.orderapi.orders.dto.OrderItemDto;
import com.systemdesign.freshcart.orderapi.orders.dto.OrderResponseDto;
import com.systemdesign.freshcart.orderapi.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.systemdesign.freshcart.orderapi.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;
import static com.systemdesign.freshcart.orderapi.rabbitmq.RabbitMqConstants.ORDER_PLACED_ROUTING_KEY;

/**
 * Publish-after-commit, for real.
 *
 * <p>The DB write happens inside {@link #saveOrder(CreateOrderDto)}, driven by a
 * {@link TransactionTemplate} rather than a {@code @Transactional} annotation on
 * {@link #placeOrder(CreateOrderDto)} itself. That distinction matters: if the whole method were
 * {@code @Transactional}, the publish call below would run <em>inside</em> the same database
 * transaction (Spring only commits when the annotated method returns), which is exactly the
 * publish-before-commit bug this design avoids. {@link TransactionTemplate#execute} returns only
 * after its callback's transaction has been committed (or rolled back), so the
 * {@code rabbitTemplate.convertAndSend} call below is textually and temporally <em>after</em>
 * commit — never nested inside it.
 *
 * <p>This mirrors the TypeScript sibling's {@code OrdersService.placeOrder}, which uses
 * {@code dataSource.transaction(async (manager) => ...)} and awaits it before building/publishing
 * the event.
 *
 * <p>If we published first (or inside the transaction, before commit), a consumer could race
 * ahead of the write: inventory-consumer could look up an order that a concurrent
 * {@code GET /orders/:id} — or a slower read replica — can't see yet. Publishing after commit
 * guarantees every consumer reacting to {@code order.placed} is reacting to a fact that is
 * already true and durable, not a promise that it will be. See the README "Ordering" section for
 * why this is the one ordering guarantee FreshCart actually needs.
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orders;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orders, TransactionTemplate transactionTemplate,
                         RabbitTemplate rabbitTemplate) {
        this.orders = orders;
        this.transactionTemplate = transactionTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public OrderResponseDto placeOrder(CreateOrderDto dto) {
        Order order = saveOrder(dto);
        // <-- transactionTemplate.execute() above has already committed — everything below only
        // runs because that call returned successfully.

        OrderPlacedEvent event = toEvent(order);
        rabbitTemplate.convertAndSend(GROCERY_EVENTS_EXCHANGE, ORDER_PLACED_ROUTING_KEY, event,
                message -> {
                    message.getMessageProperties().setContentType("application/json");
                    message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                    return message;
                });

        log.info("Order {} committed and order.placed published (eventId={}). order-api does "
                + "not know, and will never know, which consumers (if any) act on it.",
                order.getId(), event.eventId());

        return OrderResponseDto.from(order);
    }

    /** Runs entirely inside its own transaction, committed by the time {@code execute} returns. */
    private Order saveOrder(CreateOrderDto dto) {
        return transactionTemplate.execute(status -> {
            BigDecimal totalAmount = dto.items().stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Order order = new Order();
            order.setCustomerId(dto.customerId());
            order.setTotalAmount(totalAmount);
            order.setStatus("placed");
            for (OrderItemDto itemDto : dto.items()) {
                OrderItem item = new OrderItem();
                item.setSku(itemDto.sku());
                item.setName(itemDto.name());
                item.setQuantity(itemDto.quantity());
                item.setUnitPrice(itemDto.unitPrice());
                order.addItem(item);
            }

            return orders.save(order);
        });
    }

    public OrderResponseDto getOrder(UUID id) {
        Order order = orders.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        return OrderResponseDto.from(order);
    }

    private OrderPlacedEvent toEvent(Order order) {
        List<OrderPlacedEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderPlacedEvent.Item(item.getSku(), item.getName(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        OrderPlacedEvent.Payload payload = new OrderPlacedEvent.Payload(
                order.getId(), order.getCustomerId(), items, order.getTotalAmount());
        return new OrderPlacedEvent(UUID.randomUUID(), "order.placed", Instant.now(), payload);
    }
}
