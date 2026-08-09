package com.systemdesign.freshcart.notificationconsumer.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.systemdesign.freshcart.notificationconsumer.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;
import static com.systemdesign.freshcart.notificationconsumer.rabbitmq.RabbitMqConstants.NOTIFICATION_QUEUE;
import static com.systemdesign.freshcart.notificationconsumer.rabbitmq.RabbitMqConstants.ORDER_PLACED_ROUTING_KEY;

/**
 * Own exchange assertion (idempotent) + own queue, bound to the same {@code order.placed}
 * routing key that inventory-consumer, analytics-consumer, and loyalty-consumer bind to. Four
 * separate queues on one exchange is what makes this fan-out instead of task distribution: every
 * queue gets its own full copy of each event.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange groceryEventsExchange() {
        return new TopicExchange(GROCERY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange groceryEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(groceryEventsExchange).with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
