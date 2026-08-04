package com.systemdesign.modularmonolith.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants.DOMAIN_EVENTS_EXCHANGE;
import static com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants.INVENTORY_ORDER_CREATED_QUEUE;
import static com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants.NOTIFICATIONS_ORDER_CREATED_QUEUE;
import static com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants.ORDER_CREATED;

/**
 * Declares the {@code domain_events} topic exchange and every consumer's durable queue + binding
 * as code (topology-as-code), mirroring {@code rabbitmq.module.ts}
 * ({@code @golevelup/nestjs-rabbitmq}) plus the {@code @RabbitSubscribe} declarations in
 * {@code inventory.consumer.ts} / {@code notifications.consumer.ts}.
 *
 * There is deliberately no queue bound to {@code order.cancelled}: nothing in this codebase
 * consumes it (same as the NestJS source), so a message published on that routing key is simply
 * dropped by the exchange -- normal, harmless AMQP behavior for a topic exchange with no matching
 * binding.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue inventoryOrderCreatedQueue() {
        return QueueBuilder.durable(INVENTORY_ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue notificationsOrderCreatedQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding inventoryOrderCreatedBinding(Queue inventoryOrderCreatedQueue,
                                                 TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(inventoryOrderCreatedQueue)
                .to(domainEventsExchange)
                .with(ORDER_CREATED);
    }

    @Bean
    public Binding notificationsOrderCreatedBinding(Queue notificationsOrderCreatedQueue,
                                                      TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(notificationsOrderCreatedQueue)
                .to(domainEventsExchange)
                .with(ORDER_CREATED);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Overrides Spring Boot's autoconfigured RabbitTemplate so that publishing uses JSON (our
     * EventEnvelope records aren't java.io.Serializable, which the default converter requires).
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    /**
     * Backing executor for EventBus#publish's fire-and-forget send -- keeps the publish call off
     * the calling (HTTP request) thread, mirroring the non-awaited
     * `void this.eventBus.publish(...)` call in ordering.service.ts.
     */
    @Bean
    public Executor eventBusExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
