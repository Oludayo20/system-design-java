package com.systemdesign.bookhive.notification.notifications;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Held only in memory (see {@link NotificationsService}) - restarting this container wipes its
 * history; that is an acceptable, intentional tradeoff for a demo notification log, not an
 * oversight.
 */
public record Notification(
        String id,
        String type,
        String userId,
        String message,
        Map<String, Object> metadata,
        OffsetDateTime sentAt) {
}
