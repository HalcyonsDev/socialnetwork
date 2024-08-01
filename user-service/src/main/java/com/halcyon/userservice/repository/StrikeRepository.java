package com.halcyon.userservice.repository;

import com.halcyon.userservice.model.Strike;
import com.halcyon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrikeRepository extends JpaRepository<Strike, Long> {
    boolean existsByOwnerAndTarget(User owner, User target);
    List<Strike> findAllByOwner(User owner);
    List<Strike> findAllByTarget(User target);
}
