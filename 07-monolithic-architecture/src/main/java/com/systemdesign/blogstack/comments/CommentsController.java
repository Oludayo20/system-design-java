package com.systemdesign.blogstack.comments;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import com.systemdesign.blogstack.comments.dto.CommentResponse;
import com.systemdesign.blogstack.comments.dto.CreateCommentRequest;
import com.systemdesign.blogstack.comments.entity.Comment;
import com.systemdesign.blogstack.shared.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "comments", description = "Add/list comments on a post -- reading is public, adding requires a Bearer token")
@RestController
@RequestMapping("/posts/{id}/comments")
public class CommentsController {

    private final CommentsService commentsService;

    public CommentsController(CommentsService commentsService) {
        this.commentsService = commentsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Add a comment to a post",
            description = "Validates the post exists, saves the comment, and synchronously notifies the post owner "
                    + "-- all three steps run in one request against three different modules' services.")
    public CommentResponse create(
            @Parameter(description = "Post UUID") @PathVariable UUID id,
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateCommentRequest dto) {
        return toResponse(commentsService.create(id, user.userId(), dto));
    }

    @GetMapping
    @Operation(summary = "List comments on a post", description = "Public, no auth required.")
    public List<CommentResponse> findAllForPost(@Parameter(description = "Post UUID") @PathVariable UUID id) {
        return commentsService.findAllForPost(id).stream().map(CommentsController::toResponse).toList();
    }

    private static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getUserId(), comment.getBody(), comment.getCreatedAt());
    }
}
