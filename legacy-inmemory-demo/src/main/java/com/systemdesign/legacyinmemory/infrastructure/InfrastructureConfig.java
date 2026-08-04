package com.systemdesign.legacyinmemory.infrastructure;

import com.systemdesign.legacyinmemory.modules.ordering.OrderCreatedJob;
import com.systemdesign.legacyinmemory.modules.ordering.SaleEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Java port of the shared infrastructure wiring at the top of {@code bootstrap.js}:
 *
 * <pre>
 * const db = new Database('postgres');
 * const cache = new Cache();
 * const orderQueue = new Queue('orders');       // doc.md: "RabbitMQ at the Bottom"
 * const saleTopic = new Topic('sales');          // doc.md: Kafka-style broadcast topic
 * </pre>
 *
 * Each is a singleton Spring bean, shared by every module the same way the original passes
 * the same {@code db}/{@code cache}/{@code orderQueue}/{@code saleTopic} instances into every
 * {@code create*Module(...)} call.
 */
@Configuration
public class InfrastructureConfig {

    @Bean
    public InMemoryDatabase database() {
        return new InMemoryDatabase("postgres");
    }

    @Bean
    public InMemoryCache cache() {
        return new InMemoryCache();
    }

    @Bean
    public EventQueue<OrderCreatedJob> orderQueue() {
        return new EventQueue<>("orders");
    }

    @Bean
    public Topic<SaleEvent> saleTopic() {
        return new Topic<>("sales");
    }
}
