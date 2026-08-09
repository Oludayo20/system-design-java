package com.systemdesign.freshcart.loyaltyconsumer.rabbitmq;

/**
 * This is the "day 2" queue. It binds to the exact same exchange and routing key order-api has
 * been publishing to since day 1 — declaring the same exchange here is safe even though
 * order-api already created it; declaring the same exchange twice with the same arguments is a
 * no-op. Nothing about order-api's code, its Dockerfile, or its deployment changed to make this
 * consumer possible. That is the whole point of pub/sub over a broker: loyalty-consumer opted in
 * by binding a queue, order-api never opted anyone in.
 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String GROCERY_EVENTS_EXCHANGE = "grocery_events";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String LOYALTY_QUEUE = "loyalty.order-placed.queue";
}
