package com.halcyon.mediaservice.repository;

import com.halcyon.mediaservice.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOwnerId(long ownerId);
    Page<Post> findAllByOwnerIdNotIn(List<Integer> subscriptions, Pageable pageable);
    Page<Post> findAllByOwnerIdIn(List<Integer> subscriptions, Pageable pageable);
}
