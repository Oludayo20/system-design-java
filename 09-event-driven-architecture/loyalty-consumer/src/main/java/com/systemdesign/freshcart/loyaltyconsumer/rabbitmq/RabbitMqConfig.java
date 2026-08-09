package com.systemdesign.freshcart.loyaltyconsumer.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;
import static com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.RabbitMqConstants.LOYALTY_QUEUE;
import static com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.RabbitMqConstants.ORDER_PLACED_ROUTING_KEY;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange groceryEventsExchange() {
        return new TopicExchange(GROCERY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue loyaltyQueue() {
        return QueueBuilder.durable(LOYALTY_QUEUE).build();
    }

    @Bean
    public Binding loyaltyBinding(Queue loyaltyQueue, TopicExchange groceryEventsExchange) {
        return BindingBuilder.bind(loyaltyQueue).to(groceryEventsExchange).with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
