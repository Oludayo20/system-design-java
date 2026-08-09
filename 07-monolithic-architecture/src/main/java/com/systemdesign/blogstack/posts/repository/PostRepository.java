package com.systemdesign.blogstack.posts.repository;

import com.systemdesign.blogstack.posts.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    default List<Post> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
