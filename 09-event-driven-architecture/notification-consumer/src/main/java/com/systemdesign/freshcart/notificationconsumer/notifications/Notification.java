package com.systemdesign.freshcart.notificationconsumer.notifications;

import java.time.Instant;
import java.util.UUID;

public record Notification(UUID id, UUID orderId, String customerId, String message, Instant sentAt) {
}
