package com.systemdesign.faas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.systemdesign.faas.functions.CreateOrderFunction;
import com.systemdesign.faas.functions.DailySalesReportFunction;
import com.systemdesign.faas.functions.ProcessPaymentQueueMessageFunction;
import com.systemdesign.faas.functions.ResizeProductImageFunction;
import com.systemdesign.faas.runtime.ExecutionEnvironmentManager;
import com.systemdesign.faas.runtime.ExecutionEnvironmentManagerOptions;
import com.systemdesign.faas.store.OrderStore;

/**
 * Wires the plain-Java {@link ExecutionEnvironmentManager} into Spring: registers every function
 * against a factory that constructs a fresh instance on each cold start, and starts the
 * background sweeper. The manager class itself has zero Spring annotations and no Spring
 * dependency at all — see {@code ExecutionEnvironmentManagerTest}, which exercises it directly
 * with {@code new}, no application context required.
 */
@Configuration
public class RuntimeConfig {

    @Bean(destroyMethod = "stopSweeper")
    public ExecutionEnvironmentManager executionEnvironmentManager(
            @Value("${faas.warm-ttl-ms}") long warmTtlMs,
            @Value("${faas.cold-start-latency-ms}") long coldStartLatencyMs,
            OrderStore orderStore) {

        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(warmTtlMs, coldStartLatencyMs, 5_000));

        manager.register("createOrder", () -> new CreateOrderFunction(orderStore));
        manager.register("resizeProductImage", ResizeProductImageFunction::new);
        manager.register("dailySalesReport", () -> new DailySalesReportFunction(orderStore));
        manager.register("processPaymentQueueMessage", ProcessPaymentQueueMessageFunction::new);

        manager.startSweeper();
        return manager;
    }
}
