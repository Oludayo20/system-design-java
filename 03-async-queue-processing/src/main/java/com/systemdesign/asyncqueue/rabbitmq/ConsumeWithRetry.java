package com.systemdesign.asyncqueue.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * Wires a queue up to a handler with the retry/DLQ decision baked in — a direct port of
 * src/common/rabbitmq/consume-with-retry.ts onto a Spring AMQP
 * {@link SimpleMessageListenerContainer} in manual-ack mode (chosen over {@code @RabbitListener}
 * + Spring Retry because the original's retry delay is enforced by RabbitMQ itself via a queue
 * TTL + dead-letter-exchange, not by an in-process backoff loop — see the README's "RabbitMQ
 * topology" section for why that distinction matters):
 *
 * <ul>
 *   <li>handler succeeds -&gt; ack.</li>
 *   <li>handler throws, attempts remaining -&gt; republish to {@code ${queue}.retry} (its TTL
 *       redelivers the message to the main queue after RETRY_TTL_MS, with no application timer
 *       involved) and ack the original delivery.</li>
 *   <li>handler throws, attempts exhausted -&gt; republish to {@code ${queue}.dead-letter} and
 *       ack.</li>
 * </ul>
 *
 * We always ack the original message ourselves (never nack-requeue) because the redelivery is
 * done explicitly via the retry queue — leaving the message on the main queue would just spin it
 * back to the front of the same queue with no delay and no attempt cap.
 */
public final class ConsumeWithRetry {

    private ConsumeWithRetry() {
    }

    public static <T> SimpleMessageListenerContainer build(
            ConnectionFactory connectionFactory,
            RabbitmqService rabbitmq,
            ObjectMapper objectMapper,
            Topology.WorkQueueDefinition queue,
            int prefetchCount,
            Class<T> payloadType,
            ThrowingHandler<T> handler,
            Logger logger) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queue.name());
        container.setPrefetchCount(prefetchCount);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener((ChannelAwareMessageListener) (Message message, Channel channel) -> {
            MessageProperties messageProperties = message.getMessageProperties();
            Map<String, Object> headers = messageProperties.getHeaders();
            long deliveryTag = messageProperties.getDeliveryTag();

            try {
                T payload = objectMapper.readValue(message.getBody(), payloadType);
                handler.handle(payload);
                channel.basicAck(deliveryTag, false);
                return;
            } catch (Exception error) {
                int attempt = RetryUtil.nextAttempt(headers);
                String reason = error.getMessage() != null ? error.getMessage() : error.toString();
                Map<String, Object> nextHeaders = new HashMap<>(headers);
                nextHeaders.put(Topology.RETRY_COUNT_HEADER, attempt);
                nextHeaders.put(Topology.LAST_ERROR_HEADER, reason);

                if (RetryUtil.shouldDeadLetter(attempt, Topology.MAX_DELIVERY_ATTEMPTS)) {
                    logger.error("{}: attempt {}/{} failed ({}) — routing to {}",
                            queue.name(), attempt, Topology.MAX_DELIVERY_ATTEMPTS, reason, queue.deadLetterName());
                    rabbitmq.sendToQueue(queue.deadLetterName(), message.getBody(), nextHeaders);
                } else {
                    logger.warn("{}: attempt {}/{} failed ({}) — retrying via {} in {}ms",
                            queue.name(), attempt, Topology.MAX_DELIVERY_ATTEMPTS, reason, queue.retryName(),
                            Topology.RETRY_TTL_MS);
                    rabbitmq.sendToQueue(queue.retryName(), message.getBody(), nextHeaders);
                }

                try {
                    channel.basicAck(deliveryTag, false);
                } catch (IOException ackError) {
                    logger.error("Failed to ack {} delivery {}", queue.name(), deliveryTag, ackError);
                }
            }
        });

        return container;
    }
}
