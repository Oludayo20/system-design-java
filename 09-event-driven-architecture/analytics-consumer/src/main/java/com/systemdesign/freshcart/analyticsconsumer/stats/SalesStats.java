package com.systemdesign.freshcart.analyticsconsumer.stats;

import java.util.UUID;

public record SalesStats(int ordersToday, double revenueToday, UUID lastOrderId, String lastUpdatedAt) {
}
