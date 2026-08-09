package com.systemdesign.bookhive.notification.notifications;

import com.systemdesign.bookhive.notification.notifications.dto.NotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "notifications")
@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private static final Logger log = LoggerFactory.getLogger(NotificationsController.class);

    private final NotificationsService notificationsService;

    public NotificationsController(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a notification (called by order-service, fire-and-forget)")
    public Notification create(@Valid @RequestBody NotificationRequest dto) {
        Notification notification = notificationsService.record(dto);
        // Mirrors the source Express service's console.log line - the closest thing this demo
        // has to a delivery log.
        log.info("sent: {} -> {}", notification.type(), notification.userId());
        return notification;
    }

    @GetMapping
    @Operation(summary = "Inspect everything \"sent\" so far")
    public List<Notification> findAll() {
        return notificationsService.findAll();
    }
}
