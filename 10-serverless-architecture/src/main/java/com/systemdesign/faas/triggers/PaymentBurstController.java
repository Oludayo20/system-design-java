package com.systemdesign.faas.triggers;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.systemdesign.faas.config.RabbitConfig;

/**
 * Demo tooling, not itself a trigger: publishes a burst of fake payment messages onto
 * {@code payment-queue} so the queue-trigger concurrency demo doesn't require a separate script
 * or a real payment system feeding the queue.
 */
@Tag(name = "simulate", description = "Demo-only endpoints that feed real triggers")
@RestController
public class PaymentBurstController {

    private final RabbitTemplate rabbitTemplate;

    public PaymentBurstController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Operation(
            summary = "Publish a burst of N payment messages to payment-queue",
            description = "Feeds the real queue trigger so you can observe automatic concurrency scaling in the logs.")
    @PostMapping("/_simulate/payment-burst")
    public ResponseEntity<Map<String, Object>> burst(@RequestParam(defaultValue = "10") int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> message = Map.of(
                    "messageId", "msg-" + System.currentTimeMillis() + "-" + i,
                    "orderId", "order-" + i,
                    "amount", ThreadLocalRandom.current().nextInt(10_000));
            rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_QUEUE, message);
        }
        return ResponseEntity.ok(Map.of("published", count, "queue", RabbitConfig.PAYMENT_QUEUE));
    }
}
