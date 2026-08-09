package com.systemdesign.blogstack.notifications;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import com.systemdesign.blogstack.notifications.dto.NotificationResponse;
import com.systemdesign.blogstack.notifications.entity.Notification;
import com.systemdesign.blogstack.shared.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "notifications", description = "Read-only view of the current user's notifications -- requires a Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;

    public NotificationsController(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "List the current user's notifications",
            description = "Useful here to prove CommentsService -> NotificationsService.notifyNewComment() actually wrote a row.")
    public List<NotificationResponse> findMine(@CurrentUser AuthenticatedUser user) {
        return notificationsService.findForUser(user.userId()).stream().map(NotificationsController::toResponse).toList();
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
