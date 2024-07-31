package com.halcyon.userservice.repository;

import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByOwnerAndTarget(User owner, User target);
    List<Subscription> findAllByOwner(User owner);
    List<Subscription> findAllByTarget(User target);
    void deleteByOwnerAndTarget(User owner, User target);
}
