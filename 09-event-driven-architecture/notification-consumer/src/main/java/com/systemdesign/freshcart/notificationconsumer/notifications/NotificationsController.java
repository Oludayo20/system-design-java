package com.systemdesign.freshcart.notificationconsumer.notifications;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationsController {

    private final NotificationsService notificationsService;

    public NotificationsController(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    /** Inspection endpoint: every push notification sent, newest first. */
    @GetMapping("/notifications")
    public List<Notification> getAll() {
        return notificationsService.getAll();
    }
}
