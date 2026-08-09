package com.systemdesign.blogstack.notifications;

import com.systemdesign.blogstack.notifications.entity.Notification;
import com.systemdesign.blogstack.notifications.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationsService {

    private final NotificationRepository notifications;

    public NotificationsService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * Simulated notification: just a row in the {@code notifications} table (no real email/push
     * provider). Called synchronously, in-process, by
     * {@link com.systemdesign.blogstack.comments.CommentsService} right after it saves a comment
     * -- same request, same call stack, same failure domain. If this insert throws, it throws
     * inside the comment-creation request, because that's what "no event bus, no queue" means in
     * a plain monolith.
     */
    public Notification notifyNewComment(UUID recipientId, String message) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setMessage(message);
        Notification saved = notifications.save(notification);
        log.info("Notified {}: {}", recipientId, message);
        return saved;
    }

    public List<Notification> findForUser(UUID userId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId);
    }
}
