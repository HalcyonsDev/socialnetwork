package com.halcyon.authservice.service;

import com.halcyon.authservice.dto.Login2FADto;
import com.halcyon.authservice.dto.Verify2FADto;
import com.halcyon.authservice.exception.InvalidCredentialsException;
import com.halcyon.authservice.exception.TwoFactorIsNotRequiredException;
import com.halcyon.authservice.payload.SaveSecretMessage;
import com.halcyon.authservice.payload.Setup2FAResponse;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.rediscache.CacheManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTests {
    @Value("${private.secret}")
    private String privateSecret;

    @Mock
    private UserClient userClient;

    @Mock
    private AuthenticatedDataProvider authenticatedDataProvider;

    @Mock
    private UserActionsProducer userActionsProducer;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private TwoFactorAuthService twoFactorAuthService;

    private static PrivateUserResponse user;

    private static final String INVALID_OTP = "00000";

    @BeforeAll
    static void beforeAll() {
        user = PrivateUserResponse.builder()
                .username("test_username")
                .email("test_user@gmail.com")
                .about("test_about")
                .password("TestPassword123")
                .secret("testSecret")
                .isVerified(true)
                .build();
    }

    @Test
    void setup() {
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        doNothing().when(userActionsProducer).executeSaveSecret(any(SaveSecretMessage.class));

        Setup2FAResponse response = twoFactorAuthService.setup();
        assertThat(response).isNotNull();
        assertThat(response.getQrCodeUrl()).isNotNull();
    }

    @Test
    void setup_userIsBanned() {
        user.setBanned(true);
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> twoFactorAuthService.setup());
        assertThat(bannedUserException.getMessage()).isEqualTo("You are banned.");

        user.setBanned(false);
    }

    @Test
    void setup_userIsNotVerified() {
        user.setVerified(false);
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> twoFactorAuthService.setup());
        assertThat(unverifiedUserException.getMessage()).isEqualTo("You are not verified. Please confirm your email.");

        user.setVerified(true);
    }

    @Test
    void verify2FA_invalidCredentials() {
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        Verify2FADto dto = new Verify2FADto(INVALID_OTP);
        InvalidCredentialsException invalidCredentialsException = assertThrows(InvalidCredentialsException.class,
                () -> twoFactorAuthService.verify2FA(dto));
        assertThat(invalidCredentialsException.getMessage()).isEqualTo("Invalid verification code (otp)");
    }

    @Test
    void login_2faIsNotRequired() {
        Login2FADto dto = new Login2FADto(user.getEmail(), INVALID_OTP);

        when(userClient.getByEmail(dto.getEmail(), privateSecret)).thenReturn(user);
        when(cacheManager.isPresent("2fa:" + user.getEmail())).thenReturn(false);

        TwoFactorIsNotRequiredException twoFactorIsNotRequiredException = assertThrows(TwoFactorIsNotRequiredException.class,
                () -> twoFactorAuthService.login(dto));
        assertThat(twoFactorIsNotRequiredException.getMessage()).isEqualTo("Two-factor authentication is not required.");
    }

    @Test
    void login_invalidCredentials() {
        Login2FADto dto = new Login2FADto(user.getEmail(), INVALID_OTP);

        when(userClient.getByEmail(dto.getEmail(), privateSecret)).thenReturn(user);
        when(cacheManager.isPresent("2fa:" + user.getEmail())).thenReturn(true);

        InvalidCredentialsException invalidCredentialsException = assertThrows(InvalidCredentialsException.class,
                () -> twoFactorAuthService.login(dto));
        assertThat(invalidCredentialsException.getMessage()).isEqualTo("Invalid verification code (otp)");
    }
}
