package com.systemdesign.faas.triggers;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.systemdesign.faas.config.RabbitConfig;
import com.systemdesign.faas.runtime.ExecutionEnvironmentManager;
import com.systemdesign.faas.runtime.FunctionStatsSnapshot;
import com.systemdesign.faas.runtime.InvokeResult;
import com.systemdesign.faas.runtime.LambdaEvent;

import jakarta.annotation.PreDestroy;

/**
 * Queue trigger, standing in for an SQS -> Lambda event source mapping: for each message
 * delivered on {@code payment-queue}, invoke {@code processPaymentQueueMessage} through the SAME
 * {@link ExecutionEnvironmentManager} the HTTP and schedule triggers use.
 *
 * <p>Each delivery is handed off to a bounded worker pool instead of being processed inline on
 * the RabbitMQ listener container's own consumer thread — that's what makes a burst of messages
 * produce genuine concurrent invocations: the execution-environment manager sees N calls with
 * nothing idle yet and spins up N separate instances, the same way Lambda scales out concurrent
 * execution environments to drain an SQS backlog. Messages are acked individually, from the
 * worker thread, only once their own invocation finishes.
 */
@Component
public class QueueTrigger {

    private static final Logger log = LoggerFactory.getLogger(QueueTrigger.class);
    private static final String FUNCTION_NAME = "processPaymentQueueMessage";

    private final ExecutionEnvironmentManager manager;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(25, runnable -> {
        Thread thread = new Thread(runnable, "queue-trigger-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicInteger burstPeak = new AtomicInteger(0);
    private final AtomicLong coldStartsAtBurstStart = new AtomicLong(0);

    public QueueTrigger(ExecutionEnvironmentManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE, containerFactory = "manualAckContainerFactory")
    public void onMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        if (inFlight.get() == 0) {
            burstPeak.set(0);
            coldStartsAtBurstStart.set(coldStartsSoFar());
        }
        int current = inFlight.incrementAndGet();
        burstPeak.updateAndGet(peak -> Math.max(peak, current));

        executor.submit(() -> {
            try {
                process(message, channel, deliveryTag);
            } finally {
                if (inFlight.decrementAndGet() == 0) {
                    long coldStartsThisBurst = coldStartsSoFar() - coldStartsAtBurstStart.get();
                    log.info("[queue-trigger] burst complete — peak concurrency={}, cold starts this burst={}",
                            burstPeak.get(), coldStartsThisBurst);
                }
            }
        });
    }

    private void process(Message message, Channel channel, long deliveryTag) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getBody(), Map.class);
            InvokeResult result = manager.invoke(FUNCTION_NAME, LambdaEvent.of(payload));
            channel.basicAck(deliveryTag, false);
            log.info("[queue-trigger] message acked — cold={} billed={}ms", result.cold(), result.billedMs());
        } catch (IOException | RuntimeException e) {
            log.error("[queue-trigger] failed to process message, nacking (no requeue)", e);
            nack(channel, deliveryTag);
        }
    }

    private void nack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException ioException) {
            log.error("[queue-trigger] failed to nack message", ioException);
        }
    }

    private long coldStartsSoFar() {
        FunctionStatsSnapshot snapshot = manager.getStats().get(FUNCTION_NAME);
        return snapshot == null ? 0 : snapshot.coldStarts();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
