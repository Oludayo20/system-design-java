package com.systemdesign.freshcart.orderapi.rabbitmq;

/**
 * order-api is a publisher and nothing else. It asserts that the {@code grocery_events} exchange
 * exists (so the very first {@code POST /orders} doesn't fail against a missing exchange) and
 * publishes to it. It does NOT declare, bind, or even know the names of any queue — that is
 * entirely each consumer's business.
 *
 * Compare with {@code 03-async-queue-processing}'s {@code Topology}, where the producer's
 * topology also declares the work queues the producer expects to exist: that only makes sense
 * there because RabbitMQ is being used for point-to-point task queueing (the producer cares that
 * "the email queue" exists). Here it's pub/sub — order-api fires {@code order.placed} into the
 * exchange and walks away.
 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String GROCERY_EVENTS_EXCHANGE = "grocery_events";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
}
