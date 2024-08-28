package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.RegisterOAuth2UserDto;
import com.halcyon.userservice.dto.UpdateOAuth2UserDto;
import com.halcyon.userservice.dto.UserPasswordResetMessage;
import com.halcyon.userservice.exception.BannedUserException;
import com.halcyon.userservice.exception.UnverifiedUserException;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.payload.SaveSecretMessage;
import com.halcyon.userservice.payload.UserIsBannedMessage;
import com.halcyon.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private MailActionsProducer mailActionsProducer;

    @Mock
    private AuthProvider authProvider;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String BAD_CREDENTIALS_MESSAGE = "Bad Request Header Credentials.";

    private static final String INVALID_SECRET = "invalid_secret";

    private static User user;

    @BeforeAll
    static void beforeAll() {
        user = User.builder()
                .username("test_username")
                .email("test_user@gmail.com")
                .avatarPath("test_avatar_path")
                .isVerified(true)
                .authProvider("google")
                .build();
    }

    @Test
    void registerOAuth2User_badRequestCredentials() {
        RegisterOAuth2UserDto registerOAuth2UserDto = getRegisterOAuth2UserDto();
        BadCredentialsException badCredentialsException = assertThrows(BadCredentialsException.class,
                () -> userService.createOAuth2User(registerOAuth2UserDto, INVALID_SECRET));
        assertThat(badCredentialsException.getMessage()).isEqualTo(BAD_CREDENTIALS_MESSAGE);
    }

    private RegisterOAuth2UserDto getRegisterOAuth2UserDto() {
        return new RegisterOAuth2UserDto(
                user.getEmail(),
                user.getUsername(),
                user.getAvatarPath(),
                user.getAuthProvider()
        );
    }

    @Test
    void ban() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        User returnedUser = userService.ban(user);
        assertThat(returnedUser.isBanned()).isTrue();

        UserIsBannedMessage userIsBannedMessage = new UserIsBannedMessage(user.getUsername(), user.getEmail());
        verify(mailActionsProducer).executeSendUserIsBannedMessage(userIsBannedMessage);
        user.setBanned(false);
    }

    @Test
    void resetPassword() {
        UserPasswordResetMessage userPasswordResetMessage = getUserPasswordResetMessage();

        when(userRepository.findByEmail(userPasswordResetMessage.getEmail())).thenReturn(Optional.of(user));

        userService.resetPassword(userPasswordResetMessage);

        verify(userRepository).findByEmail(userPasswordResetMessage.getEmail());
        assertThat(user.getPassword()).isEqualTo(userPasswordResetMessage.getNewEncodedPassword());
        verify(userRepository).save(user);
    }

    private UserPasswordResetMessage getUserPasswordResetMessage() {
        return new UserPasswordResetMessage(user.getEmail(), "new_encoded_password");
    }

    @Test
    void uploadPhoto() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        String newImagePath = "new_image_path";

        mockGettingUser();
        when(fileStorageService.upload(multipartFile)).thenReturn(newImagePath);
        when(userRepository.save(user)).thenReturn(user);

        User returnedUser = userService.uploadAvatar(multipartFile);
        assertThat(returnedUser.getAvatarPath()).isEqualTo(newImagePath);

        verify(userRepository).findByEmail(user.getEmail());
        verify(fileStorageService).upload(multipartFile);
        verify(userRepository).save(user);
    }

    private void mockGettingUser() {
        when(authProvider.getSubject()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void uploadPhoto_bannedUser() {
        MultipartFile multipartFile = mock(MultipartFile.class);

        user.setBanned(true);
        mockGettingUser();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> userService.uploadAvatar(multipartFile));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        user.setBanned(false);
    }

    @Test
    void uploadPhoto_unverifiedUser() {
        MultipartFile multipartFile = mock(MultipartFile.class);

        user.setVerified(false);
        mockGettingUser();

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> userService.uploadAvatar(multipartFile));
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);

        user.setVerified(true);
    }

    @Test
    void updateUsername() {
        String newUsername = "new_username";

        mockGettingUser();
        when(userRepository.save(user)).thenReturn(user);

        User returnedUser = userService.updateUsername(newUsername);
        assertThat(returnedUser.getUsername()).isEqualTo(newUsername);

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_bannedUser() {
        user.setBanned(true);
        mockGettingUser();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> userService.updateUsername("new_username"));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);

        user.setBanned(false);
    }

    @Test
    void updateUsername_unverifiedUser() {
        user.setVerified(false);
        mockGettingUser();

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> userService.updateUsername("new_username"));
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);

        user.setVerified(true);
    }

    @Test
    void updateOAuth2User_badRequestCredentials() {
        UpdateOAuth2UserDto updateOAuth2UserDto = getUpdateOAuth2UserDto();
        BadCredentialsException badCredentialsException = assertThrows(BadCredentialsException.class,
                () -> userService.updateOAuth2User(updateOAuth2UserDto, INVALID_SECRET));
        assertThat(badCredentialsException.getMessage()).isEqualTo(BAD_CREDENTIALS_MESSAGE);
    }

    private UpdateOAuth2UserDto getUpdateOAuth2UserDto() {
        return new UpdateOAuth2UserDto(
                user.getEmail(),
                "new_username",
                "new_avatar_path"
        );
    }

    @Test
    void saveSecret() {
        SaveSecretMessage saveSecretMessage = getSaveSecretMessage();

        when(userRepository.findByEmail(saveSecretMessage.getEmail())).thenReturn(Optional.of(user));

        userService.saveSecret(saveSecretMessage);
        assertThat(user.getSecret()).isEqualTo(saveSecretMessage.getSecret());

        verify(userRepository).findByEmail(saveSecretMessage.getEmail());
        verify(userRepository).save(user);
    }

    private SaveSecretMessage getSaveSecretMessage() {
        return new SaveSecretMessage(user.getEmail(), "test_secret");
    }

    @Test
    void use2FA() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        userService.use2FA(user.getEmail());
        assertThat(user.isUsing2FA()).isTrue();

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository).save(user);
    }
}
