package com.systemdesign.asyncqueue.workers;

import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.asyncqueue.rabbitmq.ConsumeWithRetry;
import com.systemdesign.asyncqueue.rabbitmq.RabbitmqService;
import com.systemdesign.asyncqueue.rabbitmq.Topology;
import com.systemdesign.asyncqueue.rides.RideCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Receipt/Email worker from the doc's Uber example. Simulates ~600ms of real work (PDF + email
 * send), scaled down here so the demo isn't slow to watch. EMAIL_FAILURE_RATE deliberately fails
 * a configurable fraction of jobs so the retry -&gt; DLQ path is observable without having to
 * manually break anything. Direct port of src/workers/email.worker.ts.
 *
 * <p>Implements {@link SmartLifecycle} (Spring's equivalent of Nest's {@code OnModuleInit}/
 * {@code OnModuleDestroy}) so the underlying listener container starts when the worker app
 * context starts and stops cleanly on shutdown.
 */
@Component
public class EmailWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(EmailWorker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitmqService rabbitmq;
    private final ObjectMapper objectMapper;
    private final double failureRate;

    private SimpleMessageListenerContainer container;
    private volatile boolean running = false;

    public EmailWorker(
            ConnectionFactory connectionFactory,
            RabbitmqService rabbitmq,
            ObjectMapper objectMapper,
            @Value("${email.failure-rate:0.3}") double failureRate) {
        this.connectionFactory = connectionFactory;
        this.rabbitmq = rabbitmq;
        this.objectMapper = objectMapper;
        this.failureRate = failureRate;
    }

    @Override
    public void start() {
        container = ConsumeWithRetry.build(
                connectionFactory, rabbitmq, objectMapper, Topology.EMAIL_QUEUE, 10,
                RideCompletedEvent.class, this::handle, log);
        container.start();
        running = true;
        log.info("Consuming {} (simulated failure rate: {})", Topology.EMAIL_QUEUE.name(), failureRate);
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
        Sleep.sleep(600);

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new RuntimeException("Simulated email provider outage while emailing rider " + job.riderId());
        }

        log.info("Receipt emailed to rider {} for ride {} (fare ${}, {} -> {})",
                job.riderId(), job.rideId(), job.fare(), job.pickupLocation(), job.dropoffLocation());
    }
}
