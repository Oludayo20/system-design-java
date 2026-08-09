package com.systemdesign.freshcart.analyticsconsumer.rabbitmq;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String GROCERY_EVENTS_EXCHANGE = "grocery_events";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String ANALYTICS_QUEUE = "analytics.order-placed.queue";
}
