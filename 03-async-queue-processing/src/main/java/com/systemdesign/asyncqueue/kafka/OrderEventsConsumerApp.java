package com.systemdesign.asyncqueue.kafka;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

/**
 * Standalone consumer, parameterized by {@code GROUP_ID} (see the README's kafka:consume:*
 * commands). Run two or three of these against the same topic and each one logs every event
 * independently — consumer groups each track their own committed offset, unlike a RabbitMQ queue
 * where a message is removed once one worker consumes it.
 *
 * <p>Direct port of src/kafka/consumer.ts. Built on Spring Kafka's
 * {@link KafkaMessageListenerContainer}, instantiated directly (no {@code ApplicationContext})
 * so this stays a one-shot script rather than a managed bean — the listener-container class is
 * a plain POJO usable outside of Spring Boot for exactly this kind of standalone runner.
 *
 * <p>Run with: {@code GROUP_ID=inventory-group CONSUMER_LABEL=inventory-worker mvn compile exec:java
 * -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp}
 */
public final class OrderEventsConsumerApp {

    private OrderEventsConsumerApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        String groupId = System.getenv("GROUP_ID");
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalStateException("GROUP_ID env var is required, e.g. GROUP_ID=inventory-group");
        }
        String label = System.getenv().getOrDefault("CONSUMER_LABEL", groupId);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", KafkaConstants.brokers()));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ContainerProperties containerProperties = new ContainerProperties(KafkaConstants.ORDER_EVENTS_TOPIC);
        ObjectMapper objectMapper = new ObjectMapper();

        containerProperties.setMessageListener((MessageListener<String, String>) record -> {
            try {
                JsonNode event = objectMapper.readTree(record.value());
                System.out.printf(
                        "[%s] received %s orderId=%s partition=%d offset=%d — processed independently of the other consumer groups%n",
                        label, event.path("type").asText(), event.path("orderId").asText(),
                        record.partition(), record.offset());
            } catch (Exception e) {
                System.err.printf("[%s] failed to process record at offset %d%n", label, record.offset());
                e.printStackTrace();
            }
        });

        KafkaMessageListenerContainer<String, String> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setBeanName("order-events-consumer-" + groupId);
        container.start();

        System.out.printf("[%s] subscribed to \"%s\" as consumer group \"%s\"%n",
                label, KafkaConstants.ORDER_EVENTS_TOPIC, groupId);

        // Standalone script: block forever, same as the original Node process just running its
        // consumer.run({ eachMessage }) loop until killed.
        Thread.currentThread().join();
    }
}
