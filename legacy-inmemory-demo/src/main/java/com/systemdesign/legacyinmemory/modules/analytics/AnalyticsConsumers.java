package com.systemdesign.legacyinmemory.modules.analytics;

import com.systemdesign.legacyinmemory.infrastructure.Delay;
import com.systemdesign.legacyinmemory.infrastructure.Topic;
import com.systemdesign.legacyinmemory.modules.ordering.SaleEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Java port of {@code modules/analytics/analytics.worker.js} - doc.md: "Kafka focuses on:
 * Broadcasting events ... Order Created -&gt; Kafka -&gt; Inventory -&gt; Analytics -&gt; Fraud
 * Detection -&gt; Recommendation Engine. Many services can consume the same event
 * independently."
 *
 * <p>Both consumer groups below receive EVERY event on {@code saleTopic} - unlike the queue
 * workers, they do not compete for it (see {@link Topic}).
 */
@Component
public class AnalyticsConsumers {

    private final Topic<SaleEvent> saleTopic;

    public AnalyticsConsumers(Topic<SaleEvent> saleTopic) {
        this.saleTopic = saleTopic;
    }

    @PostConstruct
    public void register() {
        saleTopic.subscribe("analytics-service", payload -> {
            Delay.sleep(150);
            System.out.println("  [Analytics] recorded sale of $" + payload.total() + " for order #" + payload.orderId());
        });

        saleTopic.subscribe("fraud-detection-service", payload -> {
            Delay.sleep(220);
            System.out.println("  [Fraud Detection] scanned order #" + payload.orderId() + " - looks fine");
        });
    }
}
