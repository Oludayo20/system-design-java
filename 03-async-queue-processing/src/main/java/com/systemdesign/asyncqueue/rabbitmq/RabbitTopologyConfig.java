package com.systemdesign.asyncqueue.rabbitmq;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Idempotent topology declaration — Spring AMQP's {@code RabbitAdmin} auto-declares every
 * {@link Declarable} bean found in the context on startup (and on reconnect), which is the
 * direct equivalent of {@code assertTopology(channel)} in topology.ts being called from both the
 * API (as a publisher, so the exchange exists before the first POST /rides) and the worker (as a
 * consumer). Uses only core AMQP 0-9-1 features (a second exchange + per-queue TTL/DLX
 * arguments) rather than the delayed-message-exchange plugin, so it works against any stock
 * RabbitMQ image — same design note as the original.
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public Declarables ridesTopology() {
        List<Declarable> declarables = new ArrayList<>();

        TopicExchange rideEvents = new TopicExchange(Topology.RIDE_EVENTS_EXCHANGE, true, false);
        DirectExchange rideEventsDlx = new DirectExchange(Topology.RIDE_EVENTS_DLX, true, false);
        declarables.add(rideEvents);
        declarables.add(rideEventsDlx);

        for (Topology.WorkQueueDefinition queue : Topology.ALL_WORK_QUEUES) {
            Queue mainQueue = new Queue(queue.name(), true);

            // Redelivery path: when a retry-queue message's TTL expires, RabbitMQ republishes it
            // to ride_events.dlx with routing key === queue.name, which is bound straight back to
            // queue.name (see the binding below).
            Queue retryQueue = QueueBuilder.durable(queue.retryName())
                    .withArgument("x-message-ttl", (int) Topology.RETRY_TTL_MS)
                    .withArgument("x-dead-letter-exchange", Topology.RIDE_EVENTS_DLX)
                    .withArgument("x-dead-letter-routing-key", queue.name())
                    .build();

            Queue deadLetterQueue = new Queue(queue.deadLetterName(), true);

            declarables.add(mainQueue);
            declarables.add(retryQueue);
            declarables.add(deadLetterQueue);
            declarables.add(BindingBuilder.bind(mainQueue).to(rideEvents).with(Topology.RIDE_COMPLETED_ROUTING_KEY));
            declarables.add(BindingBuilder.bind(mainQueue).to(rideEventsDlx).with(queue.name()));
        }

        return new Declarables(declarables);
    }
}
