package com.systemdesign.asyncqueue.kafka;

import java.util.List;

/** Direct port of src/kafka/constants.ts. */
public final class KafkaConstants {

    private KafkaConstants() {
    }

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    public static final List<String> CONSUMER_GROUPS = List.of("inventory-group", "analytics-group", "fraud-group");

    /** Mirrors src/kafka/client.ts's brokers env lookup. */
    public static List<String> brokers() {
        String raw = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
        return List.of(raw.split(","));
    }
}
