package com.systemdesign.freshcart.loyaltyconsumer.points;

import java.util.List;

public record PointsSummary(List<CustomerPoints> customers, int processedEventCount) {
}
