package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.SubscriptionDto;
import com.halcyon.userservice.exception.BannedUserException;
import com.halcyon.userservice.exception.SubscriptionAlreadyExistsException;
import com.halcyon.userservice.exception.SubscriptionNotFoundException;
import com.halcyon.userservice.exception.UnverifiedUserException;
import com.halcyon.userservice.model.Subscription;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTests {
    @Mock
    private AuthProvider authProvider;

    @Mock
    private UserService userService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final String SUBSCRIPTION_ALREADY_EXISTS_MESSAGE = "You have already subscribed to this user";
    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "Unverified users do not have the option to subscribe. Please confirm your email.";
    private static final String SUCCESSFULLY_UNSUBSCRIBED_MESSAGE = "You have successfully unsubscribed.";
    private static final String SUBSCRIPTION_NOT_FOUND_MESSAGE = "Subscription with this owner and target is not found.";

    private static User owner;
    private static User target;

    @BeforeAll
    static void beforeAll() {
        owner = User.builder()
                .username("owner_username")
                .email("owner_user@gmail.com")
                .avatarPath("owner_avatar_path")
                .isVerified(true)
                .authProvider("google")
                .strikes(new ArrayList<>())
                .build();

        target = User.builder()
                .username("target_username")
                .email("target_user@gmail.com")
                .avatarPath("target_avatar_path")
                .isVerified(true)
                .authProvider("google")
                .strikes(new ArrayList<>())
                .build();
    }

    @Test
    void subscribe() {
        mockSubscribing();

        Subscription returnedSubscription = subscriptionService.subscribe(getSubscriptionDto());

        assertThat(returnedSubscription).isNotNull();
        assertThat(returnedSubscription.getTarget()).isEqualTo(target);
        assertThat(returnedSubscription.getOwner()).isEqualTo(owner);
    }

    private void mockSubscribing() {
        mockGettingUsers();
        when(subscriptionRepository.existsByOwnerAndTarget(owner, target)).thenReturn(false);

        Subscription subscription = getSubscription();
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);
    }

    private void mockGettingUsers() {
        mockGettingOwner();
        when(userService.findByEmail(target.getEmail())).thenReturn(target);
    }

    private void mockGettingOwner() {
        when(authProvider.getSubject()).thenReturn(owner.getEmail());
        when(userService.findByEmail(owner.getEmail())).thenReturn(owner);
    }

    private Subscription getSubscription() {
        return new Subscription(owner, target);
    }

    private SubscriptionDto getSubscriptionDto() {
        return new SubscriptionDto(target.getEmail());
    }

    @Test
    void subscribe_alreadyExists() {
        mockGettingUsers();
        when(subscriptionRepository.existsByOwnerAndTarget(owner, target)).thenReturn(true);

        SubscriptionDto subscriptionDto = getSubscriptionDto();
        SubscriptionAlreadyExistsException subscriptionAlreadyExistsException = assertThrows(SubscriptionAlreadyExistsException.class,
                () -> subscriptionService.subscribe(subscriptionDto));
        assertThat(subscriptionAlreadyExistsException.getMessage()).isEqualTo(SUBSCRIPTION_ALREADY_EXISTS_MESSAGE);
    }

    @Test
    void subscribe_bannedUser() {
        owner.setBanned(true);
        mockGettingOwner();

        SubscriptionDto subscriptionDto = getSubscriptionDto();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> subscriptionService.subscribe(subscriptionDto));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void subscribe_unverifiedUser() {
        owner.setVerified(false);
        mockGettingOwner();

        SubscriptionDto subscriptionDto = getSubscriptionDto();
        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> subscriptionService.subscribe(subscriptionDto));
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);

        owner.setVerified(true);
    }

    @Test
    void unsubscribe() {
        mockUnsubscribing();

        String response = subscriptionService.unsubscribe(getSubscriptionDto());

        assertThat(response)
                .isNotNull()
                .isEqualTo(SUCCESSFULLY_UNSUBSCRIBED_MESSAGE);
    }

    private void mockUnsubscribing() {
        mockGettingUsers();
        when(subscriptionRepository.existsByOwnerAndTarget(owner, target)).thenReturn(true);
        doNothing().when(subscriptionRepository).deleteByOwnerAndTarget(owner, target);
    }

    @Test
    void unsubscribe_notFound() {
        mockGettingUsers();
        when(subscriptionRepository.existsByOwnerAndTarget(owner, target)).thenReturn(false);

        SubscriptionDto subscriptionDto = getSubscriptionDto();
        SubscriptionNotFoundException subscriptionNotFoundException = assertThrows(SubscriptionNotFoundException.class,
                () -> subscriptionService.unsubscribe(subscriptionDto));
        assertThat(subscriptionNotFoundException.getMessage()).isEqualTo(SUBSCRIPTION_NOT_FOUND_MESSAGE);
    }

    @Test
    void unsubscribe_bannedUser() {
        owner.setBanned(true);
        mockGettingOwner();

        SubscriptionDto subscriptionDto = getSubscriptionDto();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> subscriptionService.unsubscribe(subscriptionDto));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void findSubscriptionsByOwnerId() {
        mockGettingOwner();
        when(userService.findById(1)).thenReturn(owner);

        subscriptionService.findSubscriptionsByOwnerId(1);
        verify(subscriptionRepository).findAllByOwner(owner);
    }

    @Test
    void getSubscriptions() {
        mockGettingOwner();
        subscriptionService.findSubscriptions();
        verify(subscriptionRepository).findAllByOwner(owner);
    }

    @Test
    void findSubscriptionsByOwnerId_bannedUser() {
        owner.setBanned(true);
        mockGettingOwner();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> subscriptionService.findSubscriptionsByOwnerId(1));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void findSubscribers() {
        mockGettingOwner();
        subscriptionService.findSubscribers();
        verify(subscriptionRepository).findAllByTarget(owner);
    }

    @Test
    void findSubscribersByTargetId() {
        mockGettingOwner();
        when(userService.findById(1)).thenReturn(target);

        subscriptionService.findSubscribersByTargetId(1);
        verify(subscriptionRepository).findAllByTarget(target);
    }

    @Test
    void findSubscribersByOwnerId_bannedUser() {
        owner.setBanned(true);
        mockGettingOwner();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> subscriptionService.findSubscribersByTargetId(1));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        owner.setBanned(false);
    }
}
