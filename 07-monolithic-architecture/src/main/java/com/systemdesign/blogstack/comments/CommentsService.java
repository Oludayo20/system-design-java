package com.systemdesign.blogstack.comments;

import com.systemdesign.blogstack.comments.dto.CreateCommentRequest;
import com.systemdesign.blogstack.comments.entity.Comment;
import com.systemdesign.blogstack.comments.repository.CommentRepository;
import com.systemdesign.blogstack.notifications.NotificationsService;
import com.systemdesign.blogstack.posts.PostsService;
import com.systemdesign.blogstack.posts.entity.Post;
import com.systemdesign.blogstack.users.UsersService;
import com.systemdesign.blogstack.users.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The property the README calls "everything is connected": to add one comment, this class
 * injects {@link PostsService}, {@link UsersService}, and {@link NotificationsService} directly
 * and calls plain, synchronous, in-process methods on all three -- no event bus, no HTTP, no
 * queue between any of them. Every call runs inside the same request and the same call stack. If
 * {@code NotificationsService.notifyNewComment()} throws, the whole request fails and the
 * comment is never returned to the caller, even though the comment row and the notification row
 * are unrelated concerns. Mirrors {@code src/modules/comments/comments.service.ts}.
 */
@Service
public class CommentsService {

    private final CommentRepository comments;
    private final PostsService postsService;
    private final UsersService usersService;
    private final NotificationsService notificationsService;

    public CommentsService(CommentRepository comments, PostsService postsService,
                            UsersService usersService, NotificationsService notificationsService) {
        this.comments = comments;
        this.postsService = postsService;
        this.usersService = usersService;
        this.notificationsService = notificationsService;
    }

    public Comment create(UUID postId, UUID userId, CreateCommentRequest dto) {
        Post post = postsService.findById(postId); // throws NotFoundException if missing
        User author = usersService.findById(userId); // fetched only for the notification text

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setBody(dto.body());
        Comment saved = comments.save(comment);

        if (!post.getUserId().equals(userId)) {
            notificationsService.notifyNewComment(
                    post.getUserId(),
                    author.getDisplayName() + " commented on your post \"" + post.getTitle() + "\"");
        }

        return saved;
    }

    public List<Comment> findAllForPost(UUID postId) {
        postsService.findById(postId); // 404s if the post doesn't exist
        return comments.findByPostIdOrderByCreatedAtAsc(postId);
    }
}
