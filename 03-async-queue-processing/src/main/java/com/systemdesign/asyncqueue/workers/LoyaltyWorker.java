package com.systemdesign.asyncqueue.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.asyncqueue.rabbitmq.ConsumeWithRetry;
import com.systemdesign.asyncqueue.rabbitmq.RabbitmqService;
import com.systemdesign.asyncqueue.rabbitmq.Topology;
import com.systemdesign.asyncqueue.rides.RideCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** Loyalty worker: awards points for the completed ride. Direct port of src/workers/loyalty.worker.ts. */
@Component
public class LoyaltyWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyWorker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitmqService rabbitmq;
    private final ObjectMapper objectMapper;

    private SimpleMessageListenerContainer container;
    private volatile boolean running = false;

    public LoyaltyWorker(ConnectionFactory connectionFactory, RabbitmqService rabbitmq, ObjectMapper objectMapper) {
        this.connectionFactory = connectionFactory;
        this.rabbitmq = rabbitmq;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        container = ConsumeWithRetry.build(
                connectionFactory, rabbitmq, objectMapper, Topology.LOYALTY_QUEUE, 10,
                RideCompletedEvent.class, this::handle, log);
        container.start();
        running = true;
        log.info("Consuming {}", Topology.LOYALTY_QUEUE.name());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void handle(RideCompletedEvent job) {
        Sleep.sleep(150);
        long points = Math.max(1, Math.round(Double.parseDouble(job.fare())));
        log.info("Awarded {} loyalty points to rider {} for ride {}", points, job.riderId(), job.rideId());
    }
}
