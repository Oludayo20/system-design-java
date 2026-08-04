package com.systemdesign.asyncqueue.kafka;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Standalone script (plain {@code main()}, not part of either Spring Boot app's component scan
 * or {@code ApplicationContext} — deliberately outside the DI container, exactly like the
 * original's comment on src/kafka/client.ts): publishes {@code order.created} events to Kafka so
 * multiple independent consumer groups can each read the full stream. Contrast with RabbitMQ's
 * ride.completed above, where a job goes to exactly one worker.
 *
 * <p>Direct port of src/kafka/producer.ts. Uses Spring Kafka's {@link KafkaTemplate} constructed
 * by hand (no {@code ApplicationContext} needed) rather than the raw {@code kafka-clients}
 * {@code KafkaProducer}, per this project's convention of using spring-kafka for the Kafka
 * producer/consumer pieces.
 *
 * <p>Run with: {@code mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsProducerApp}
 */
public final class OrderEventsProducerApp {

    private OrderEventsProducerApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", KafkaConstants.brokers()));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        ObjectMapper objectMapper = new ObjectMapper();

        int count = Integer.parseInt(System.getenv().getOrDefault("EVENT_COUNT", "10"));
        long intervalMs = Long.parseLong(System.getenv().getOrDefault("EVENT_INTERVAL_MS", "1000"));

        System.out.printf("[kafka-producer] publishing %d order.created events to \"%s\"%n",
                count, KafkaConstants.ORDER_EVENTS_TOPIC);

        try {
            for (int i = 0; i < count; i++) {
                String orderId = UUID.randomUUID().toString();

                Map<String, Object> event = new LinkedHashMap<>();
                event.put("type", "order.created");
                event.put("orderId", orderId);
                event.put("customerId", "customer-" + (int) Math.ceil(Math.random() * 1000));
                event.put("total", Math.round((Math.random() * 200 + 10) * 100.0) / 100.0);
                event.put("createdAt", Instant.now().toString());

                String json = objectMapper.writeValueAsString(event);
                template.send(KafkaConstants.ORDER_EVENTS_TOPIC, orderId, json).get();
                System.out.printf("[kafka-producer] published order.created orderId=%s%n", orderId);

                if (i < count - 1) {
                    Thread.sleep(intervalMs);
                }
            }
        } catch (Exception e) {
            System.err.println("[kafka-producer] fatal error");
            e.printStackTrace();
            producerFactory.destroy();
            System.exit(1);
        }

        producerFactory.destroy();
        System.out.println("[kafka-producer] done");
    }
}
