package com.halcyon.mediaservice.repository;

import com.halcyon.mediaservice.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOwnerEmail(String ownerEmail);
}
