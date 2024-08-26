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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static com.halcyon.userservice.util.UserUtil.isUserBanned;
import static com.halcyon.userservice.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthProvider authProvider;
    private final MailActionsProducer mailActionsProducer;
    private final FileStorageService fileStorageService;
    private final JwtProvider jwtProvider;

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";

    public void create(CreateUserDto dto) {
        save(
                User.builder()
                        .email(dto.getEmail())
                        .username(dto.getUsername())
                        .about(dto.getAbout())
                        .authProvider("local")
                        .password(dto.getPassword())
                        .build()
        );
    }

    public User registerOAuth2User(RegisterOAuth2UserDto dto) {
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

    public User ban(User user) {
        user.setBanned(true);
        user = save(user);

        UserIsBannedMessage message = new UserIsBannedMessage(user.getUsername(), user.getEmail());
        mailActionsProducer.executeSendUserIsBannedMessage(message);

        return user;
    }

    private User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with this email not found."));
    }

    public User findById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with this id not found."));
    }

    public User findByToken(String token) {
        String email = jwtProvider.extractEmail(token);
        return findByEmail(email);
    }

    public void resetPassword(UserPasswordResetMessage userPasswordResetMessage) {
        User user = findByEmail(userPasswordResetMessage.getEmail());
        user.setPassword(userPasswordResetMessage.getNewEncodedPassword());

        save(user);
    }

    public void verifyByEmail(String email) {
        User user = findByEmail(email);

        user.setVerified(true);
        save(user);
    }

    public void changeEmail(ChangeEmailMessage changeEmailMessage) {
        User user = findByEmail(changeEmailMessage.getCurrentEmail());
        user.setEmail(changeEmailMessage.getNewEmail());

        save(user);
    }

    public User uploadAvatar(MultipartFile imageFile) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        String imagePath = fileStorageService.upload(imageFile);
        user.setAvatarPath(imagePath);

        return save(user);
    }

    public File getMyAvatar() {
        User user = findByEmail(authProvider.getSubject());
        return fileStorageService.getFileByPath(user.getAvatarPath());
    }

    public File getAvatar(String email) {
        return fileStorageService.getFileByPath(findByEmail(email).getAvatarPath());
    }

    public User updateUsername(String username) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        user.setUsername(username);

        return save(user);
    }

    public User updateAbout(String about) {
        User user = findByEmail(authProvider.getSubject());
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, UNVERIFIED_USER_MESSAGE);

        user.setAbout(about);

        return save(user);
    }

    public User updateOAuth2User(UpdateOAuth2UserDto dto) {
        User user = findByEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setAvatarPath(dto.getAvatarUrl());

        return save(user);
    }

    public void saveSecret(SaveSecretMessage saveSecretMessage) {
        User user = findByEmail(saveSecretMessage.getEmail());
        user.setSecret(saveSecretMessage.getSecret());
        save(user);
    }

    public void use2FA(String email) {
        User user = findByEmail(email);
        user.setUsing2FA(true);
        save(user);
    }
}
