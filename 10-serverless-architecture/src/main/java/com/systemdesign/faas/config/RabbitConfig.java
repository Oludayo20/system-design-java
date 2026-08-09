package com.systemdesign.faas.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Declares {@code payment-queue} (durable — the queue trigger's target) and a manual-ack listener
 * container factory. {@code QueueTrigger} acks/nacks each message itself, from a worker thread,
 * only once its own {@code ExecutionEnvironmentManager.invoke(...)} call finishes — necessary
 * because each delivery is fanned out onto a thread pool rather than handled inline on the
 * container's own consumer thread. A generous prefetch lets many messages be in flight to that
 * pool at once, which is what makes a burst produce genuine concurrent invocations instead of
 * being serialized by RabbitMQ's default unacked-message limit.
 */
@Configuration
public class RabbitConfig {

    public static final String PAYMENT_QUEUE = "payment-queue";

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Overrides Spring Boot's autoconfigured RabbitTemplate so publishing uses JSON, not Java serialization. */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(50);
        return factory;
    }
}
