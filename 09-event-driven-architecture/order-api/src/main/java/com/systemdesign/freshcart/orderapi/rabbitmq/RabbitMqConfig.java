package com.systemdesign.freshcart.orderapi.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.systemdesign.freshcart.orderapi.rabbitmq.RabbitMqConstants.GROCERY_EVENTS_EXCHANGE;

/**
 * Declares the {@code grocery_events} topic exchange only — no queues, no bindings. A topic
 * exchange (rather than fanout) is used so this can grow to more event types later
 * ({@code order.cancelled}, {@code order.refunded}, ...) with each consumer choosing exactly
 * which routing keys it wants, without every consumer being forced to receive every event type
 * the way a true fanout exchange would. Today every consumer binds only {@code order.placed}, so
 * it behaves identically to fanout in practice — the choice is about where the ceiling is, not
 * what happens on day one.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange groceryEventsExchange() {
        return new TopicExchange(GROCERY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
