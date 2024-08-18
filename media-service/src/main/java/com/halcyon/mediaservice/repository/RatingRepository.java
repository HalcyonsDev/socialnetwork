package com.halcyon.mediaservice.repository;

import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByOwnerIdAndPost(long ownerId, Post post);
    @Query("SELECT r FROM Rating r WHERE r.post = :post AND r.isLike = :isLike")
    Page<Rating> findAllByPostAndIsLike(@Param("post") Post post, @Param("isLike") boolean isLike, Pageable pageable);
    void deleteAllByPost(Post post);
}
