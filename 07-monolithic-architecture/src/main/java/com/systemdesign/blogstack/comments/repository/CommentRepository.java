package com.systemdesign.blogstack.comments.repository;

import com.systemdesign.blogstack.comments.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
