package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.UserPasswordResetEvent;
import com.halcyon.userservice.exception.UserNotFoundException;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.payload.ChangeEmailMessage;
import com.halcyon.userservice.payload.SaveSecretMessage;
import com.halcyon.userservice.payload.Use2FAMessage;
import com.halcyon.userservice.payload.UserIsBannedMessage;
import com.halcyon.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static com.halcyon.userservice.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthProvider authProvider;
    private final MailActionsProducer mailActionsProducer;
    private final FileStorageService fileStorageService;

    public void create(CreateUserDto dto) {
        userRepository.save(
                User.builder()
                        .email(dto.getEmail())
                        .username(dto.getUsername())
                        .about(dto.getAbout())
                        .password(dto.getPassword())
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

    public void resetPassword(UserPasswordResetEvent passwordResetEvent) {
        User user = findByEmail(passwordResetEvent.getEmail());
        user.setPassword(passwordResetEvent.getNewEncodedPassword());

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

    public User uploadPhoto(MultipartFile imageFile) {
        User user = findByEmail(authProvider.getSubject());
        isUserVerified(user, "You are not verified. Please confirm your email.");

        String imagePath = fileStorageService.upload(imageFile);
        user.setAvatarPath(imagePath);

        return save(user);
    }

    public File getAvatar() {
        User user = findByEmail(authProvider.getSubject());
        return fileStorageService.getFileByPath(user.getAvatarPath());
    }

    public File getAvatar(String email) {
        return fileStorageService.getFileByPath(findByEmail(email).getAvatarPath());
    }

    public User updateUsername(String username) {
        User user = findByEmail(authProvider.getSubject());
        isUserVerified(user, "You are not verified. Please confirm your email.");

        user.setUsername(username);

        return save(user);
    }

    public User updateAbout(String about) {
        User user = findByEmail(authProvider.getSubject());
        isUserVerified(user, "You are not verified. Please confirm your email.");

        user.setAbout(about);

        return save(user);
    }

    public void saveSecret(SaveSecretMessage saveSecretMessage) {
        User user = findByEmail(saveSecretMessage.getEmail());
        user.setSecret(saveSecretMessage.getSecret());
        save(user);
    }

    public void use2FA(Use2FAMessage use2FAMessage) {
        User user = findByEmail(use2FAMessage.getEmail());
        user.setUsing2FA(true);
        save(user);
    }
}
