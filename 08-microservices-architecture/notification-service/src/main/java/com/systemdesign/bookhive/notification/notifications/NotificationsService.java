package com.systemdesign.bookhive.notification.notifications;

import com.systemdesign.bookhive.notification.notifications.dto.NotificationRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deliberately no database. As long as nothing else in the system depends on this service
 * staying up (see order-service's fire-and-forget, short-timeout call), an in-memory list is
 * all this lesson needs.
 */
@Service
public class NotificationsService {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Notification record(NotificationRequest dto) {
        Notification notification = new Notification(
                String.valueOf(nextId.getAndIncrement()),
                dto.type(),
                dto.userId(),
                dto.message(),
                dto.metadata(),
                OffsetDateTime.now());
        notifications.add(notification);
        return notification;
    }

    public List<Notification> findAll() {
        return Collections.unmodifiableList(notifications);
    }
}
