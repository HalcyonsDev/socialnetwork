package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.RegisterOAuth2UserDto;
import com.halcyon.userservice.dto.UpdateOAuth2UserDto;
import com.halcyon.userservice.dto.UserPasswordResetMessage;
import com.halcyon.userservice.exception.UserNotFoundException;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.payload.*;
import com.halcyon.userservice.repository.UserRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;

import static com.halcyon.userservice.util.UserUtil.isUserBanned;
import static com.halcyon.userservice.util.UserUtil.isUserVerified;

/**
 * Service class for handling user-related operations
 *
 * @author Ruslan Sadikov
 */
@Service
@RequiredArgsConstructor
public class UserService {
    @Value("${private.secret}")
    private String privateSecret;

    private final UserRepository userRepository;
    private final AuthProvider authProvider;
    private final MailActionsProducer mailActionsProducer;
    private final FileStorageService fileStorageService;
    private final JwtProvider jwtProvider;

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";

    /**
     * Creates and saves {@link #save(User)} a new user {@link User} based on the provided {@link CreateUserDto}
     * Triggered when a message is received from the auth-service {@link UserActionsConsumer#listenCreatingUser(String)}.
     *
     * @param dto the {@link CreateUserDto} data transfer object containing the user information to be saved
     */
    public void create(CreateUserDto dto) {
        save(
                User.builder()
                        .email(dto.getEmail())
                        .username(dto.getUsername())
                        .about(dto.getAbout())
                        .authProvider("local")
                        .password(dto.getPassword())
                        .strikes(new ArrayList<>())
                        .subscribers(new ArrayList<>())
                        .subscriptions(new ArrayList<>())
                        .build()
        );
    }

    private User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Creates and saves {@link #save} a new user {@link User} using the provided OAuth2 provider details {@link RegisterOAuth2UserDto}.
     *
     * @param dto the {@link RegisterOAuth2UserDto} data transfer object containing the user's OAuth2 provider information
     * @param authSecretKey the secret key used for validation to ensure the request is allowed.
     * @return the saved {@link User} entity
     */
    public User createOAuth2User(RegisterOAuth2UserDto dto, String authSecretKey) {
        isValidPrivateSecret(authSecretKey);
        return save(
            User.builder()
                    .email(dto.getEmail())
                    .username(dto.getUsername())
                    .avatarPath(dto.getAvatarUrl())
                    .authProvider(dto.getAuthProvider())
                    .isVerified(true)
                    .build()
        );
    }

    private void isValidPrivateSecret(String authSecretKey) {
        if (StringUtils.isBlank(authSecretKey) || !authSecretKey.equals(privateSecret)) {
            throw new BadCredentialsException("Bad Request Header Credentials.");
        }
    }

    /**
     * Updates an existing OAuth2 user {@link User} with new details and saves him to the database {@link #save(User)}.
     *
     * @param dto the {@link UpdateOAuth2UserDto} data transfer object containing the user's OAuth2 provider information
     * @param authSecretKey the secret key used for validation to ensure the request is allowed.
     * @return the updated and saved {@link User} entity
     */
    public User updateOAuth2User(UpdateOAuth2UserDto dto, String authSecretKey) {
        isValidPrivateSecret(authSecretKey);

        User user = findByEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setAvatarPath(dto.getAvatarUrl());

        return save(user);
    }

    /**
     * Bans a user by setting him banned status to {@code true}
     * and saving the updated {@link User} entity by calling {@link #save(User)}.
     * It also sends a {@link UserIsBannedMessage} to notification-service
     * by calling {@link MailActionsProducer#executeSendUserIsBannedMessage(UserIsBannedMessage)}.
     *
     * @param user the {@link User} entity to be banned
     * @return the updated and saved {@link User} entity with the banned status set to {@code true}
     */
    public User ban(User user) {
        user.setBanned(true);
        user = save(user);

        sendUserIsBannedMessage(user);

        return user;
    }

    private void sendUserIsBannedMessage(User user) {
        UserIsBannedMessage message = new UserIsBannedMessage(user.getUsername(), user.getEmail());
        mailActionsProducer.executeSendUserIsBannedMessage(message);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getByEmail(String email, String authSecretKey) {
        isValidPrivateSecret(authSecretKey);
        return findByEmail(email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with this email not found."));
    }

    public User getById(long userId, String authSecretKey) {
        isValidPrivateSecret(authSecretKey);
        return findById(userId);
    }

    public User findById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with this id not found."));
    }

    /**
     * Retrieves a {@link User} entity associated with the provided JWT token.
     *
     * @param token the JWT token from which the user's email address is to be extracted
     * @return a {@link User} entity associated with the email extracted from the token
     */
    public User findByToken(String token) {
        String email = jwtProvider.extractEmail(token);
        return findByEmail(email);
    }

    /**
     * Resets the password of a {@link User} based on the provided {@link UserPasswordResetMessage}
     * and saves him by calling {@link #save(User)}.
     * Triggered when a message is received from the auth-service {@link UserActionsConsumer#listenResetPassword(String)}.
     *
     * @param userPasswordResetMessage a message containing the user's email and the new encoded password
     */
    public void resetPassword(UserPasswordResetMessage userPasswordResetMessage) {
        User user = findByEmail(userPasswordResetMessage.getEmail());
        user.setPassword(userPasswordResetMessage.getNewEncodedPassword());

        save(user);
    }

    /**
     * Verifies the {@link User} associated with the given email address.
     * Triggered when a message is received from the auth-service {@link UserActionsConsumer#listenVerifyByEmail(String)}.
     *
     * @param email the email address of the user to verify
     */
    public void verifyByEmail(String email) {
        User user = findByEmail(email);

        user.setVerified(true);
        save(user);
    }

    /**
     * Changes the email address of the {@link User} based on the provided {@link ChangeEmailMessage}
     * Triggered when a message is received from the auth-service {@link UserActionsConsumer#listenChangeEmail(String)}.
     *
     * @param changeEmailMessage the {@link ChangeEmailMessage} containing the user's current email and the new email to update to
     */
    public void changeEmail(ChangeEmailMessage changeEmailMessage) {
        User user = findByEmail(changeEmailMessage.getCurrentEmail());
        user.setEmail(changeEmailMessage.getNewEmail());

        save(user);
    }

    /**
     * Uploads an avatar image for the authenticated {@link User} by calling {@link FileStorageService#upload(MultipartFile)}
     * and updates his profile with the new avatar path.
     *
     * @param imageFile the {@link MultipartFile} representing the avatar image to be uploaded
     * @return the updated {@link User} entity with the new avatar path
     * @throws com.halcyon.userservice.exception.BannedUserException if the user is banned
     * @throws com.halcyon.userservice.exception.UnverifiedUserException if the user is not verified
     */
    public User uploadAvatar(MultipartFile imageFile) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        String imagePath = fileStorageService.upload(imageFile);
        user.setAvatarPath(imagePath);

        return save(user);
    }

    /**
     * Retrieves the avatar file for the currently authenticated {@link User}
     * by calling {@link FileStorageService#getFileByPath(String)}
     * by getting his email from JWT token provided in header {@link AuthProvider}.
     *
     * @return {@link File} representing the avatar of the authenticated user
     */
    public File getMyAvatar() {
        User user = findByEmail(authProvider.getSubject());
        return fileStorageService.getFileByPath(user.getAvatarPath());
    }

    /**
     * Retrieves the avatar file for a specified {@link User} based on his email.
     *
     * @param email the email address of the user whose avatar is to be retrieved
     * @return the {@link File} representing the avatar of the user with the specified email
     */
    public File getAvatar(String email) {
        return fileStorageService.getFileByPath(findByEmail(email).getAvatarPath());
    }

    /**
     * Updates the username of the currently authenticated {@link User}.
     *
     * @param username the new username to be set for the authenticated user
     * @return the updated {@link User} with the new username
     * @throws com.halcyon.userservice.exception.BannedUserException if user is banned
     * @throws com.halcyon.userservice.exception.UnverifiedUserException if user is not verified
     */
    public User updateUsername(String username) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        user.setUsername(username);

        return save(user);
    }

    /**
     * Updates "about me" of the currently authenticated {@link User}.
     *
     * @param about the new "about me" to be set for the authenticated user
     * @return the updated {@link User} with the new "about"
     * @throws com.halcyon.userservice.exception.BannedUserException if user is banned
     * @throws com.halcyon.userservice.exception.UnverifiedUserException if user is not verified
     */
    public User updateAbout(String about) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        user.setAbout(about);

        return save(user);
    }

    /**
     * Saves two-factor authentication (2FA) secret for a specified {@link User} based on his email.
     * Triggered when a message is received from the auth-service {@link UserActionsConsumer#listenSaveSecret(String)}.
     *
     * @param saveSecretMessage the {@link SaveSecretMessage} containing the user's email address and the 2fa-secret to save to
     */
    public void saveSecret(SaveSecretMessage saveSecretMessage) {
        User user = findByEmail(saveSecretMessage.getEmail());
        user.setSecret(saveSecretMessage.getSecret());
        save(user);
    }

    /**
     * Enables two-factor authentication (2FA) for the specified {@link User}.
     *
     * @param email the email address of the user for whom 2FA should be enabled
     */
    public void use2FA(String email) {
        User user = findByEmail(email);
        user.setUsing2FA(true);
        save(user);
    }
}
