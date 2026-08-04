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

/**
 * Analytics worker: records the completed sale for reporting/dashboards. Does not touch Postgres
 * in this reference implementation — in a real system this would write to a warehouse/analytics
 * store, which is exactly the kind of slow, non-critical work this pattern exists to keep off the
 * request path. Direct port of src/workers/analytics.worker.ts.
 */
@Component
public class AnalyticsWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsWorker.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitmqService rabbitmq;
    private final ObjectMapper objectMapper;

    private SimpleMessageListenerContainer container;
    private volatile boolean running = false;

    public AnalyticsWorker(ConnectionFactory connectionFactory, RabbitmqService rabbitmq, ObjectMapper objectMapper) {
        this.connectionFactory = connectionFactory;
        this.rabbitmq = rabbitmq;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        container = ConsumeWithRetry.build(
                connectionFactory, rabbitmq, objectMapper, Topology.ANALYTICS_QUEUE, 10,
                RideCompletedEvent.class, this::handle, log);
        container.start();
        running = true;
        log.info("Consuming {}", Topology.ANALYTICS_QUEUE.name());
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
        Sleep.sleep(200);
        log.info("Recorded sale analytics for ride {}: fare ${}", job.rideId(), job.fare());
    }
}
