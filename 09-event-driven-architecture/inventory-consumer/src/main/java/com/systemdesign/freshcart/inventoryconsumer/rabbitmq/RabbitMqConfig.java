package com.systemdesign.freshcart.inventoryconsumer.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.systemdesign.freshcart.inventoryconsumer.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;
import static com.systemdesign.freshcart.inventoryconsumer.rabbitmq.RabbitMqConstants.INVENTORY_QUEUE;
import static com.systemdesign.freshcart.inventoryconsumer.rabbitmq.RabbitMqConstants.ORDER_PLACED_ROUTING_KEY;

/**
 * inventory-consumer asserts the exchange too (idempotent — safe even if order-api hasn't started
 * yet) plus its OWN queue, bound to {@code order.placed}. This queue belongs to
 * inventory-consumer alone: notification-consumer, analytics-consumer, and loyalty-consumer each
 * bind their own separate queue to the same exchange/routing key, so all four receive an
 * independent copy of every {@code order.placed} event — true fan-out (pub/sub), not four workers
 * competing for one shared queue's messages the way {@code email.queue} works in
 * {@code 03-async-queue-processing}.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange groceryEventsExchange() {
        return new TopicExchange(GROCERY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(INVENTORY_QUEUE).build();
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, TopicExchange groceryEventsExchange) {
        return BindingBuilder.bind(inventoryQueue).to(groceryEventsExchange).with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
