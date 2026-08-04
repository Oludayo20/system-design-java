package com.systemdesign.asyncqueue.rabbitmq;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Thin publish/send wrapper around {@link RabbitTemplate}, mirroring
 * src/common/rabbitmq/rabbitmq.service.ts's {@code publish}/{@code sendToQueue} pair.
 */
@Service
public class RabbitmqService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitmqService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /** Publish to an exchange (fan-out entry point, e.g. ride_events / ride.completed). */
    public void publish(String exchange, String routingKey, Object payload, Map<String, Object> headers) {
        rabbitTemplate.send(exchange, routingKey, buildMessage(toJsonBytes(payload), headers));
    }

    public void publish(String exchange, String routingKey, Object payload) {
        publish(exchange, routingKey, payload, Map.of());
    }

    /** Publish straight to a named queue — used for retry/dead-letter requeueing. */
    public void sendToQueue(String queue, byte[] content, Map<String, Object> headers) {
        // Default exchange ("") + routing key == queue name is the standard AMQP 0-9-1
        // direct-to-queue publish, exactly what amqplib's channel.sendToQueue does under the hood.
        rabbitTemplate.send("", queue, buildMessage(content, headers));
    }

    private byte[] toJsonBytes(Object payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize RabbitMQ payload", e);
        }
    }

    private Message buildMessage(byte[] body, Map<String, Object> headers) {
        MessagePropertiesBuilder builder = MessagePropertiesBuilder.newInstance()
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        if (headers != null) {
            headers.forEach(builder::setHeader);
        }
        return new Message(body, builder.build());
    }
}
