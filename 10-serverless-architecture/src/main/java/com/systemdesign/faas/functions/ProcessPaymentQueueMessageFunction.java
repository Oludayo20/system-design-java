package com.systemdesign.faas.functions;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systemdesign.faas.runtime.LambdaContext;
import com.systemdesign.faas.runtime.LambdaEvent;
import com.systemdesign.faas.runtime.LambdaFunction;
import com.systemdesign.faas.runtime.LambdaResponse;

/**
 * Queue-triggered function: one invocation per RabbitMQ message on {@code payment-queue},
 * standing in for an SQS -> Lambda event source mapping. When several messages arrive in a burst,
 * {@code QueueTrigger} invokes this function concurrently (not one-at-a-time), which is what lets
 * {@code ExecutionEnvironmentManager} demonstrate automatic concurrency scaling.
 */
public class ProcessPaymentQueueMessageFunction implements LambdaFunction {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentQueueMessageFunction.class);

    public ProcessPaymentQueueMessageFunction() {
        ColdInit.simulateWork(30);
        log.info("[processPaymentQueueMessage] cold init: payment processor instance constructed");
    }

    @Override
    public LambdaResponse handle(LambdaEvent event, LambdaContext context) {
        // Simulated payment-processing work (charge validation, ledger write, etc.).
        long workMs = 80 + ThreadLocalRandom.current().nextLong(120);
        ColdInit.simulateWork(workMs);

        Object messageId = event.get("messageId");
        Object orderId = event.get("orderId");
        Object amount = event.get("amount");

        log.info("[processPaymentQueueMessage] processed messageId={} orderId={} amount={} instance={}",
                messageId, orderId, amount, context.requestId());

        return new LambdaResponse(200, Map.of("processed", true, "messageId", String.valueOf(messageId)));
    }
}
