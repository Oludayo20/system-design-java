package com.systemdesign.blogstack.comments;

import com.systemdesign.blogstack.comments.dto.CreateCommentRequest;
import com.systemdesign.blogstack.comments.entity.Comment;
import com.systemdesign.blogstack.comments.repository.CommentRepository;
import com.systemdesign.blogstack.notifications.NotificationsService;
import com.systemdesign.blogstack.posts.PostsService;
import com.systemdesign.blogstack.posts.entity.Post;
import com.systemdesign.blogstack.shared.exception.NotFoundException;
import com.systemdesign.blogstack.users.UsersService;
import com.systemdesign.blogstack.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ports {@code src/modules/comments/comments.service.spec.ts}. The whole point of this project is
 * that {@link CommentsService} calls {@link PostsService}, {@link UsersService}, and
 * {@link NotificationsService} as plain, synchronous, in-process method calls -- not HTTP, not
 * events. This test proves those calls happen (and don't happen) exactly when the plain-monolith
 * design says they should, using Mockito to stand in for the other three modules' services --
 * same lightweight, no-Spring-context style as {@code CircuitBreakerTest} in 05-resilience.
 */
@ExtendWith(MockitoExtension.class)
class CommentsServiceTest {

    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID COMMENTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostsService postsService;

    @Mock
    private UsersService usersService;

    @Mock
    private NotificationsService notificationsService;

    private CommentsService commentsService;

    @BeforeEach
    void setUp() {
        commentsService = new CommentsService(commentRepository, postsService, usersService, notificationsService);
    }

    private static Post postOwnedBy(UUID ownerId) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setUserId(ownerId);
        post.setTitle("A post about monoliths");
        return post;
    }

    private static User author(UUID id, String displayName) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(displayName);
        return user;
    }

    @Test
    void callsPostsServiceAndUsersServiceDirectlyInProcess() {
        when(postsService.findById(POST_ID)).thenReturn(postOwnedBy(OWNER_ID));
        when(usersService.findById(COMMENTER_ID)).thenReturn(author(COMMENTER_ID, "Amir Musa"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentsService.create(POST_ID, COMMENTER_ID, new CreateCommentRequest("Nice post!"));

        verify(postsService).findById(POST_ID);
        verify(usersService).findById(COMMENTER_ID);
    }

    @Test
    void callsNotificationsServiceSynchronouslyBeforeReturning() {
        when(postsService.findById(POST_ID)).thenReturn(postOwnedBy(OWNER_ID));
        when(usersService.findById(COMMENTER_ID)).thenReturn(author(COMMENTER_ID, "Amir Musa"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentsService.create(POST_ID, COMMENTER_ID, new CreateCommentRequest("Nice post!"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationsService).notifyNewComment(org.mockito.ArgumentMatchers.eq(OWNER_ID), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo("Amir Musa commented on your post \"A post about monoliths\"");
        assertThat(result.getBody()).isEqualTo("Nice post!");
    }

    @Test
    void doesNotNotifyThePostOwnerWhenTheyCommentOnTheirOwnPost() {
        when(postsService.findById(POST_ID)).thenReturn(postOwnedBy(OWNER_ID));
        when(usersService.findById(OWNER_ID)).thenReturn(author(OWNER_ID, "Jane Doe"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentsService.create(POST_ID, OWNER_ID, new CreateCommentRequest("Note to self"));

        verify(notificationsService, never()).notifyNewComment(any(), any());
    }

    @Test
    void propagatesNotFoundExceptionFromPostsServiceWithoutSavingAComment() {
        when(postsService.findById(POST_ID)).thenThrow(new NotFoundException("Post " + POST_ID + " not found"));

        assertThatThrownBy(() -> commentsService.create(POST_ID, COMMENTER_ID, new CreateCommentRequest("Nice post!")))
                .isInstanceOf(NotFoundException.class);

        verify(commentRepository, never()).save(any());
        verify(notificationsService, never()).notifyNewComment(any(), any());
    }
}
