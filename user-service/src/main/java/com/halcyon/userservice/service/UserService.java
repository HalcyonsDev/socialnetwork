package com.halcyon.userservice.service;

import com.halcyon.userservice.dto.CreateUserDto;
import com.halcyon.userservice.dto.UserPasswordResetEvent;
import com.halcyon.userservice.exception.UserIsNotVerifiedException;
import com.halcyon.userservice.exception.UserNotFoundException;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.payload.ChangeEmailMessage;
import com.halcyon.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthProvider authProvider;

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

    public User updateUsername(String username) {
        User user = findByEmail(authProvider.getSubject());
        isVerifiedUser(user);

        user.setUsername(username);

        return save(user);
    }

    public User updateAbout(String about) {
        User user = findByEmail(authProvider.getSubject());
        isVerifiedUser(user);

        user.setAbout(about);

        return save(user);
    }

    private void isVerifiedUser(User user) {
        if (!user.isVerified()) {
            throw new UserIsNotVerifiedException();
        }
    }
}
