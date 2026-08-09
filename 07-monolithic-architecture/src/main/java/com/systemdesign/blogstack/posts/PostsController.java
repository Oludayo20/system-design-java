package com.systemdesign.blogstack.posts;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import com.systemdesign.blogstack.posts.dto.CreatePostRequest;
import com.systemdesign.blogstack.posts.dto.PostResponse;
import com.systemdesign.blogstack.posts.entity.Post;
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

@Tag(name = "posts", description = "Create/list/get posts -- reading is public, creating requires a Bearer token")
@RestController
@RequestMapping("/posts")
public class PostsController {

    private final PostsService postsService;

    public PostsController(PostsService postsService) {
        this.postsService = postsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a post", description = "Owned by the authenticated user.")
    public PostResponse create(@CurrentUser AuthenticatedUser user, @Valid @RequestBody CreatePostRequest dto) {
        return toResponse(postsService.create(user.userId(), dto));
    }

    @GetMapping
    @Operation(summary = "List all posts", description = "Public, no auth required.")
    public List<PostResponse> findAll() {
        return postsService.findAll().stream().map(PostsController::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a post by ID", description = "Public, no auth required.")
    public PostResponse findById(@Parameter(description = "Post UUID") @PathVariable UUID id) {
        return toResponse(postsService.findById(id));
    }

    private static PostResponse toResponse(Post post) {
        return new PostResponse(post.getId(), post.getUserId(), post.getTitle(), post.getBody(), post.getCreatedAt());
    }
}
