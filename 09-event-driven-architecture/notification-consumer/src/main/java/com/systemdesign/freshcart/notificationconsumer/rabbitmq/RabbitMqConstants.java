package com.systemdesign.freshcart.notificationconsumer.rabbitmq;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String GROCERY_EVENTS_EXCHANGE = "grocery_events";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String NOTIFICATION_QUEUE = "notification.order-placed.queue";
}
