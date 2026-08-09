package com.systemdesign.faas.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systemdesign.faas.runtime.LambdaContext;
import com.systemdesign.faas.runtime.LambdaEvent;
import com.systemdesign.faas.runtime.LambdaFunction;
import com.systemdesign.faas.runtime.LambdaResponse;
import com.systemdesign.faas.store.Order;
import com.systemdesign.faas.store.OrderItem;
import com.systemdesign.faas.store.OrderStore;

/**
 * HTTP-triggered function (mounted behind the {@code apiGateway} trigger as {@code POST /orders}).
 * Validates the request and creates an order in the shared in-memory store.
 *
 * <p>Zero Spring MVC code in here on purpose — no {@code HttpServletRequest}, no
 * {@code @RequestBody}, nothing. The {@link LambdaEvent}/{@link LambdaContext}/{@link LambdaResponse}
 * shapes are all this class knows about, which is what makes it portable to a real trigger without
 * changes.
 *
 * <p>The constructor below is this function's "cold init" work: real Lambda functions do this
 * once per fresh execution environment (open a DB pool, load config, warm a cache). Here it is
 * genuine, measurable cost — {@code ExecutionEnvironmentManager} only pays it on a cold start, by
 * constructing a brand-new {@code CreateOrderFunction}; a warm reuse skips this constructor
 * entirely.
 */
public class CreateOrderFunction implements LambdaFunction {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderFunction.class);

    private final OrderStore orderStore;
    private final List<String> requiredFields;

    public CreateOrderFunction(OrderStore orderStore) {
        this.orderStore = orderStore;
        ColdInit.simulateWork(30);
        this.requiredFields = List.of("customerId", "items");
        log.info("[createOrder] cold init: handler instance constructed");
    }

    @Override
    public LambdaResponse handle(LambdaEvent event, LambdaContext context) {
        Object customerIdRaw = event.get("customerId");
        Object itemsRaw = event.get("items");

        if (!(customerIdRaw instanceof String customerId) || customerId.isBlank()
                || !(itemsRaw instanceof List<?> rawItems) || rawItems.isEmpty()) {
            return new LambdaResponse(400, Map.of(
                    "error", requiredFields.get(0) + " and a non-empty " + requiredFields.get(1) + "[] are required"));
        }

        List<OrderItem> items = new ArrayList<>();
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> map)) {
                return new LambdaResponse(400, Map.of("error", "each item must be an object with sku and qty"));
            }
            Object sku = map.get("sku");
            Object qty = map.get("qty");
            if (!(sku instanceof String skuStr) || skuStr.isBlank() || !(qty instanceof Number qtyNum)) {
                return new LambdaResponse(400, Map.of("error", "each item must have a string sku and numeric qty"));
            }
            items.add(new OrderItem(skuStr, qtyNum.intValue()));
        }

        Order order = orderStore.addOrder(customerId, items);
        return new LambdaResponse(201, Map.of("order", order));
    }
}
