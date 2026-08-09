package com.systemdesign.faas.functions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systemdesign.faas.runtime.LambdaContext;
import com.systemdesign.faas.runtime.LambdaEvent;
import com.systemdesign.faas.runtime.LambdaFunction;
import com.systemdesign.faas.runtime.LambdaResponse;
import com.systemdesign.faas.store.OrderStore;
import com.systemdesign.faas.store.OrderSummary;

/**
 * Schedule (cron-style) triggered function. Fired on an interval by {@code ScheduleTrigger},
 * never by an HTTP request — stands in for an EventBridge scheduled rule invoking a Lambda
 * directly.
 */
public class DailySalesReportFunction implements LambdaFunction {

    private static final Logger log = LoggerFactory.getLogger(DailySalesReportFunction.class);

    private final OrderStore orderStore;

    public DailySalesReportFunction(OrderStore orderStore) {
        this.orderStore = orderStore;
        ColdInit.simulateWork(30);
        log.info("[dailySalesReport] cold init: report generator instance constructed");
    }

    @Override
    public LambdaResponse handle(LambdaEvent event, LambdaContext context) {
        OrderSummary summary = orderStore.summarize();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", Instant.now().toString());
        report.put("scheduledAt", event.get("scheduledAt"));
        report.put("ordersProcessed", summary.ordersProcessed());
        report.put("totalRevenue", summary.totalRevenue());

        log.info("[dailySalesReport] report generated: {}", report);

        return new LambdaResponse(200, report);
    }
}
