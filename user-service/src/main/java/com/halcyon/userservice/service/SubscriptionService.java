package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.SubscriptionDto;
import com.halcyon.userservice.exception.SubscriptionAlreadyExistsException;
import com.halcyon.userservice.exception.SubscriptionNotFoundException;
import com.halcyon.userservice.exception.UnverifiedUserException;
import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final AuthProvider authProvider;

    public Subscription subscribe(SubscriptionDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());

        if (!owner.isVerified()) {
            throw new UnverifiedUserException("Unverified users do not have the option to subscribe. Please confirm your email.");
        }

        User target = userService.findByEmail(dto.getTargetEmail());

        if (!target.isVerified()) {
            throw new UnverifiedUserException("You can't subscribe to an unverified user.");
        }

        if (subscriptionRepository.existsByOwnerAndTarget(owner, target)) {
            throw new SubscriptionAlreadyExistsException();
        }

        Subscription subscription = new Subscription(owner, target);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public String unsubscribe(SubscriptionDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());
        User target = userService.findByEmail(dto.getTargetEmail());

        if (!subscriptionRepository.existsByOwnerAndTarget(owner, target)) {
            throw new SubscriptionNotFoundException();
        }

        subscriptionRepository.deleteByOwnerAndTarget(owner, target);
        return "You have successfully unsubscribed.";
    }

    public List<Subscription> getSubscriptions() {
        return getSubscriptionsByEmail(authProvider.getSubject());
    }

    public List<Subscription> getSubscriptionsByEmail(String email) {
        User owner = userService.findByEmail(email);
        return subscriptionRepository.findAllByOwner(owner);
    }

    public List<Subscription> getSubscribers() {
        return getSubscribersByEmail(authProvider.getSubject());
    }

    public List<Subscription> getSubscribersByEmail(String email) {
        User target = userService.findByEmail(email);
        return subscriptionRepository.findAllByTarget(target);
    }
}
