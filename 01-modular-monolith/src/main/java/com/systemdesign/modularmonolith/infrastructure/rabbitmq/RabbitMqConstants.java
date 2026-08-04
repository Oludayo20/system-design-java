package com.systemdesign.modularmonolith.infrastructure.rabbitmq;

/**
 * Routing keys published on the {@code domain_events} topic exchange. Keep this list in sync
 * with every {@code @RabbitListener} binding in the codebase.
 *
 * Mirrors {@code src/infrastructure/rabbitmq/rabbitmq.constants.ts} from the NestJS source.
 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String DOMAIN_EVENTS_EXCHANGE = "domain_events";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";

    public static final String INVENTORY_ORDER_CREATED_QUEUE = "inventory.order_created";
    public static final String NOTIFICATIONS_ORDER_CREATED_QUEUE = "notifications.order_created";
}
