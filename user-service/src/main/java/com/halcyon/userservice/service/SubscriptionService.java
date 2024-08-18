package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.SubscriptionDto;
import com.halcyon.userservice.exception.SubscriptionAlreadyExistsException;
import com.halcyon.userservice.exception.SubscriptionNotFoundException;
import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.halcyon.userservice.util.UserUtil.isUserBanned;
import static com.halcyon.userservice.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final AuthProvider authProvider;

    public Subscription subscribe(SubscriptionDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());
        isUserVerified(owner, "Unverified users do not have the option to subscribe. Please confirm your email.");
        isUserBanned(owner, "You are banned.");

        User target = userService.findByEmail(dto.getTargetEmail());

        if (subscriptionRepository.existsByOwnerAndTarget(owner, target)) {
            throw new SubscriptionAlreadyExistsException();
        }

        Subscription subscription = new Subscription(owner, target);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public String unsubscribe(SubscriptionDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());
        isUserBanned(owner, "You are banned.");

        User target = userService.findByEmail(dto.getTargetEmail());

        if (!subscriptionRepository.existsByOwnerAndTarget(owner, target)) {
            throw new SubscriptionNotFoundException("Subscription with this owner and target is not found.");
        }

        subscriptionRepository.deleteByOwnerAndTarget(owner, target);
        return "You have successfully unsubscribed.";
    }

    public Subscription findById(long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription with this id not found."));
    }

    public List<Subscription> getSubscriptions() {
        User owner = userService.findByEmail(authProvider.getSubject());
        return subscriptionRepository.findAllByOwner(owner);
    }

    public List<Subscription> findSubscriptionsByOwnerId(long ownerId) {
        User user = userService.findByEmail(authProvider.getSubject());
        isUserBanned(user, "You are banned.");

        User owner = userService.findById(ownerId);
        return subscriptionRepository.findAllByOwner(owner);
    }

    public List<Subscription> findSubscribers() {
        User target = userService.findByEmail(authProvider.getSubject());
        return subscriptionRepository.findAllByTarget(target);
    }

    public List<Subscription> findSubscribersByOwnerId(long ownerId) {
        User user = userService.findByEmail(authProvider.getSubject());
        isUserBanned(user, "You are banned.");

        User target = userService.findById(ownerId);
        return subscriptionRepository.findAllByTarget(target);
    }
}
