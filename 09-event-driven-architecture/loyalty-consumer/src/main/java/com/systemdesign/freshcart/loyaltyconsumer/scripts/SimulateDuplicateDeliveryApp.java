package com.systemdesign.freshcart.loyaltyconsumer.scripts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.OrderPlacedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Simulates a duplicate delivery of a single order.placed event straight onto
 * loyalty-consumer's own queue — the same thing a real RabbitMQ redelivery would do if
 * loyalty-consumer crashed after applying an event but before acking it, or if the broker
 * decided a message needed redelivering after a network blip.
 *
 * <p>Deliberately publishes directly to {@code loyalty.order-placed.queue} (via the default
 * exchange, i.e. {@code channel.basicPublish("", queueName, ...)}), NOT to the
 * {@code grocery_events} exchange. Publishing to the exchange would fan the duplicate out to
 * inventory-consumer/notification-consumer/analytics-consumer too, and this demo is about
 * proving loyalty-consumer's idempotency check specifically — those three consumers don't
 * implement one (see README), so a shared/exchange-wide duplicate would double their side
 * effects and muddy the result. Publishing directly to one already-bound queue is exactly what a
 * redelivery to that queue alone looks like on the wire.
 *
 * <p>Standalone, DI-free program (plain {@code main()}, its own {@code com.rabbitmq.client}
 * connection) — mirrors the TypeScript sibling's {@code scripts/simulate-duplicate-delivery.ts},
 * and the same convention {@code 03-async-queue-processing} uses for its Kafka scripts.
 *
 * <p>Usage (from the {@code loyalty-consumer} directory):
 * <pre>
 *   mvn compile exec:java -Dexec.mainClass=com.systemdesign.freshcart.loyaltyconsumer.scripts.SimulateDuplicateDeliveryApp
 * </pre>
 *
 * <p>Then: {@code curl http://localhost:4104/points} — the demo customer's points reflect ONE
 * award, plus one entry in {@code processedEventCount}, not two, even though the message was
 * delivered twice.
 */
public final class SimulateDuplicateDeliveryApp {

    private static final String GROCERY_EVENTS_EXCHANGE = "grocery_events";
    private static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    private static final String LOYALTY_QUEUE = "loyalty.order-placed.queue";
    private static final String DEMO_CUSTOMER_ID = "demo-customer-idempotency";

    private SimulateDuplicateDeliveryApp() {
    }

    public static void main(String[] args) throws Exception {
        String rabbitmqUrl = System.getenv().getOrDefault(
                "RABBITMQ_URL", "amqp://freshcart:freshcart_password@localhost:5672");
        String loyaltyConsumerPort = System.getenv().getOrDefault("LOYALTY_CONSUMER_PORT", "4104");

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitmqUrl);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Idempotent — matches the topology loyalty-consumer itself asserts on boot.
            channel.exchangeDeclare(GROCERY_EVENTS_EXCHANGE, "topic", true);
            channel.queueDeclare(LOYALTY_QUEUE, true, false, false, null);
            channel.queueBind(LOYALTY_QUEUE, GROCERY_EVENTS_EXCHANGE, ORDER_PLACED_ROUTING_KEY);

            UUID eventId = UUID.randomUUID();
            BigDecimal totalAmount = new BigDecimal("42");
            OrderPlacedEvent.Item item = new OrderPlacedEvent.Item("rice-5kg", "Rice 5kg Bag", 1, totalAmount);
            OrderPlacedEvent.Payload payload = new OrderPlacedEvent.Payload(
                    UUID.randomUUID(), DEMO_CUSTOMER_ID, List.of(item), totalAmount);
            OrderPlacedEvent event = new OrderPlacedEvent(eventId, "order.placed", Instant.now(), payload);

            byte[] content = objectMapper.writeValueAsBytes(event);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            System.out.println("Simulating a duplicate delivery of eventId=" + eventId + " to " + LOYALTY_QUEUE);
            System.out.println("Expect: exactly one award of 42 points to " + DEMO_CUSTOMER_ID);
            System.out.println();

            System.out.println("Sending delivery #1...");
            channel.basicPublish("", LOYALTY_QUEUE, properties, content);

            Thread.sleep(500);

            System.out.println("Sending delivery #2 (identical eventId — simulates redelivery)...");
            channel.basicPublish("", LOYALTY_QUEUE, properties, content);

            Thread.sleep(500);

            System.out.println();
            System.out.println("Done. Check: curl http://localhost:" + loyaltyConsumerPort + "/points");
            System.out.println(DEMO_CUSTOMER_ID + " should show 42 points (not 84), and "
                    + "processedEventCount should have increased by exactly 1, not 2.");
        }
    }
}
