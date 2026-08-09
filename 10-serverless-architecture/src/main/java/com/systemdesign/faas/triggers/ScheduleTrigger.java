package com.systemdesign.faas.triggers;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.systemdesign.faas.runtime.ExecutionEnvironmentManager;
import com.systemdesign.faas.runtime.InvokeResult;
import com.systemdesign.faas.runtime.LambdaEvent;

/**
 * Schedule trigger, standing in for an EventBridge/CloudWatch Events cron rule (e.g.
 * {@code rate(1 day)}) invoking a Lambda directly with no HTTP request involved. Implemented with
 * a {@link ScheduledExecutorService} rather than a cron-expression library to keep the dependency
 * footprint small — the mechanic being taught (the platform invokes your function on a schedule,
 * cold-starting it if it hasn't run recently) doesn't require real cron parsing.
 */
@Component
public class ScheduleTrigger {

    private static final Logger log = LoggerFactory.getLogger(ScheduleTrigger.class);

    private final ExecutionEnvironmentManager manager;
    private final long intervalMs;
    private ScheduledExecutorService executor;

    public ScheduleTrigger(ExecutionEnvironmentManager manager,
                            @Value("${faas.schedule-interval-ms}") long intervalMs) {
        this.manager = manager;
        this.intervalMs = intervalMs;
    }

    @PostConstruct
    void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "schedule-trigger");
            thread.setDaemon(true);
            return thread;
        });
        log.info("[schedule-trigger] dailySalesReport scheduled every {}ms (stands in for a cron/rate expression)",
                intervalMs);
        executor.scheduleAtFixedRate(this::fire, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void fire() {
        try {
            InvokeResult result = manager.invoke("dailySalesReport",
                    LambdaEvent.of(Map.of("scheduledAt", Instant.now().toString())));
            log.info("[schedule-trigger] fired — cold={} duration={}ms billed={}ms",
                    result.cold(), result.durationMs(), result.billedMs());
        } catch (RuntimeException e) {
            log.error("[schedule-trigger] invocation failed", e);
        }
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
