package com.systemdesign.modularmonolith.inventory;

import com.systemdesign.modularmonolith.catalog.CatalogService;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.EventEnvelope;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants;
import com.systemdesign.modularmonolith.ordering.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * The Inventory "module": no controller, no HTTP surface -- just a durable queue bound to
 * {@code order.created}. Still the same process, same deployment, but decoupled from Ordering:
 * Ordering has no idea Inventory exists, it only knows it published an event.
 *
 * Mirrors {@code src/modules/inventory/inventory.consumer.ts}.
 */
@Slf4j
@Component
public class InventoryConsumer {

    private final CatalogService catalogService;

    public InventoryConsumer(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @RabbitListener(queues = RabbitMqConstants.INVENTORY_ORDER_CREATED_QUEUE)
    public void handleOrderCreated(EventEnvelope<OrderCreatedEvent> envelope) {
        OrderCreatedEvent payload = envelope.payload();
        log.info("Reducing stock for order {} ({} line item(s))", payload.orderId(), payload.items().size());

        for (OrderCreatedEvent.Item item : payload.items()) {
            catalogService.decrementStock(item.productId(), item.quantity());
        }

        log.info("Stock reduction complete for order {}", payload.orderId());
    }
}
