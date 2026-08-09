package com.systemdesign.blogstack.posts;

import com.systemdesign.blogstack.posts.dto.CreatePostRequest;
import com.systemdesign.blogstack.posts.entity.Post;
import com.systemdesign.blogstack.posts.repository.PostRepository;
import com.systemdesign.blogstack.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PostsService {

    private final PostRepository posts;

    public PostsService(PostRepository posts) {
        this.posts = posts;
    }

    public Post create(UUID userId, CreatePostRequest dto) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.title());
        post.setBody(dto.body());
        return posts.save(post);
    }

    public List<Post> findAll() {
        return posts.findAllNewestFirst();
    }

    /**
     * Called directly by {@link com.systemdesign.blogstack.comments.CommentsService} (in-process,
     * not over HTTP) to confirm a post exists before attaching a comment to it.
     */
    public Post findById(UUID id) {
        return posts.findById(id)
                .orElseThrow(() -> new NotFoundException("Post " + id + " not found"));
    }
}
