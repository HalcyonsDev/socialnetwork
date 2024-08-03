package com.halcyon.mediaservice.repository;

import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPost(Post post);
    void deleteAllByPost(Post post);
}
