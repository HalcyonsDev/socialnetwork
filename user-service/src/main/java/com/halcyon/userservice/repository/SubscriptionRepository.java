package com.halcyon.userservice.repository;

import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByOwnerAndTarget(User owner, User target);
    List<Subscription> findAllByOwner(User owner);
    List<Subscription> findAllByTarget(User target);
    void deleteByOwnerAndTarget(User owner, User target);

    @Query("SELECT user.id FROM User user JOIN user.subscribers subscribers WHERE subscribers.owner = :owner")
    List<Integer> findIdOfUsersSubscribedByUser(@Param("owner") User owner);
}
