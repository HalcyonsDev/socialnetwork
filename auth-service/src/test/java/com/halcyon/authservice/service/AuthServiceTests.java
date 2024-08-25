package com.halcyon.authservice.service;

import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.exception.InvalidCredentialsException;
import com.halcyon.authservice.exception.InvalidEmailException;
import com.halcyon.authservice.exception.InvalidVerificationCodeException;
import com.halcyon.authservice.exception.UserAlreadyExistsException;
import com.halcyon.authservice.payload.*;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.authservice.security.RefreshTokenGenerator;
import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.jwtlibrary.TokenRevocationService;
import com.halcyon.rediscache.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Value("${private.secret}")
    private String privateSecret;

    @Mock
    private UserClient userClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private UserActionsProducer userActionsProducer;

    @Mock
    private MailActionsProducer mailActionsProducer;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private AuthenticatedDataProvider authenticatedDataProvider;

    @InjectMocks
    private AuthService authService;

    private static PrivateUserResponse user;

    private static final String TOKEN = "test_token";
    private static final String AUTH_HEADER = "Authorization";

    @BeforeAll
    static void beforeAll() {
        user = PrivateUserResponse.builder()
                .username("test_username")
                .email("test_user@gmail.com")
                .about("test_about")
                .password("TestPassword123")
                .isVerified(true)
                .build();
    }

    @Test
    void register() {
        RegisterUserDto dto = getRegisterUserDto();

        when(userClient.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("test_hashed_password");

        doNothing().when(userActionsProducer).executeCreatingUser(dto);

        when(jwtProvider.generateAccessToken(dto.getEmail())).thenReturn(TOKEN);
        when(refreshTokenGenerator.generate(dto.getEmail())).thenReturn(TOKEN);

        VerificationMessage verificationMessage = new VerificationMessage(user.getUsername(), user.getEmail(), TOKEN);
        doNothing().when(mailActionsProducer).executeSendVerificationMessage(verificationMessage);

        AuthResponse response = authService.register(dto);
        isValidAuthResponse(response);
    }

    private RegisterUserDto getRegisterUserDto() {
        return RegisterUserDto.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .about(user.getAbout())
                .password(user.getPassword())
                .build();
    }

    private void isValidAuthResponse(AuthResponse response) {
        assertThat(response.getAccessToken()).isEqualTo(TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(TOKEN);
        assertThat(response.getTYPE()).isEqualTo("Bearer");
    }

    @Test
    void register_userAlreadyExists() {
        RegisterUserDto dto = getRegisterUserDto();

        when(userClient.existsByEmail(dto.getEmail())).thenReturn(true);

        UserAlreadyExistsException userAlreadyExistsException = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(dto));
        assertThat(userAlreadyExistsException.getMessage()).isEqualTo("User with this email already exists.");
    }

    @Test
    void login() {
        AuthRequest request = new AuthRequest(user.getEmail(), user.getPassword());

        when(userClient.getByEmail(request.getEmail(), privateSecret)).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken(request.getEmail())).thenReturn(TOKEN);
        when(refreshTokenGenerator.generate(request.getEmail())).thenReturn(TOKEN);

        AuthResponse response = authService.login(request);
        isValidAuthResponse(response);
    }

    @Test
    void login_invalidCredentials() {
        AuthRequest request = new AuthRequest(user.getEmail(), user.getPassword());

        when(userClient.getByEmail(request.getEmail(), privateSecret)).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        InvalidCredentialsException invalidCredentialsException = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request));
        assertThat(invalidCredentialsException.getMessage()).isEqualTo("Invalid credentials provided.");
    }

    @Test
    void logout() {
        mockRevokeMethod();
        assertThat(authService.logout()).isEqualTo("You have successfully logout from your account.");
    }

    private void mockRevokeMethod() {
        when(httpServletRequest.getHeader(AUTH_HEADER)).thenReturn("Bearer " + TOKEN);
        doNothing().when(tokenRevocationService).revoke(TOKEN);
    }

    @Test
    void confirmByEmail() {
        when(jwtProvider.extractEmail(TOKEN)).thenReturn(user.getEmail());
        doNothing().when(userActionsProducer).executeConfirmByEmail(user.getEmail());

        assertThat(authService.confirmByEmail(TOKEN)).isEqualTo("Account is verified.");
    }

    @Test
    void getTokensByFalseRefresh() {
        String newAccessToken = "new_access_" + TOKEN;

        when(cacheManager.fetch(TOKEN, String.class)).thenReturn(Optional.ofNullable(user.getEmail()));
        when(jwtProvider.generateAccessToken(user.getEmail())).thenReturn(newAccessToken);

        AuthResponse response = authService.getTokensByRefresh(TOKEN, false);

        assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getTYPE()).isEqualTo("Bearer");
    }

    @Test
    void getTokensByTrueRefresh() {
        String newAccessToken = "new_access_" + TOKEN;
        String newRefreshToken = "new_refresh_" + TOKEN;

        when(cacheManager.fetch(TOKEN, String.class)).thenReturn(Optional.ofNullable(user.getEmail()));
        when(jwtProvider.generateAccessToken(user.getEmail())).thenReturn(newAccessToken);
        when(refreshTokenGenerator.generate(user.getEmail())).thenReturn(newRefreshToken);

        AuthResponse response = authService.getTokensByRefresh(TOKEN, true);

        assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.getTYPE()).isEqualTo("Bearer");
    }

    @Test
    void forgotPassword() {
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
        when(jwtProvider.generateAccessToken(user.getEmail())).thenReturn(TOKEN);

        ForgotPasswordMessage forgotPasswordMessage = new ForgotPasswordMessage(user.getEmail(), TOKEN);
        doNothing().when(mailActionsProducer).executeSendForgotPasswordMessage(forgotPasswordMessage);

        assertThat(authService.forgotPassword()).isEqualTo("A link to reset your password has been sent to your email.");
    }

    @Test
    void resetPassword() {
        ResetPasswordDto dto = getResetPasswordDto();

        when(jwtProvider.extractEmail(TOKEN)).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
        when(passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(dto.getNewPassword())).thenReturn("hashed_" + dto.getNewPassword());

        UserPasswordResetMessage userPasswordResetMessage = new UserPasswordResetMessage(user.getEmail(), "hashed_" + dto.getNewPassword());
        doNothing().when(userActionsProducer).executeResetPassword(userPasswordResetMessage);

        doNothing().when(tokenRevocationService).revoke(TOKEN);

        assertThat(authService.resetPassword(dto, TOKEN)).isEqualTo("Password has been reset successfully.");
    }

    private ResetPasswordDto getResetPasswordDto() {
        return new ResetPasswordDto(user.getPassword(), "test_new_password");
    }

    @Test
    void resetPassword_userIsBanned() {
        when(jwtProvider.extractEmail(TOKEN)).thenReturn(user.getEmail());

        user.setBanned(true);
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        ResetPasswordDto resetPasswordDto = getResetPasswordDto();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> authService.resetPassword(resetPasswordDto, TOKEN));
        assertThat(bannedUserException.getMessage()).isEqualTo("You are banned.");

        user.setBanned(false);
    }

    @Test
    void resetPassword_userIsNotVerified() {
        when(jwtProvider.extractEmail(TOKEN)).thenReturn(user.getEmail());

        user.setVerified(false);
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);

        ResetPasswordDto resetPasswordDto = getResetPasswordDto();
        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> authService.resetPassword(resetPasswordDto, TOKEN));
        assertThat(unverifiedUserException.getMessage()).isEqualTo("You are not verified. Please confirm your email.");

        user.setVerified(true);
    }

    @Test
    void resetPassword_invalidCredentials() {
        ResetPasswordDto dto = getResetPasswordDto();

        when(jwtProvider.extractEmail(TOKEN)).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), privateSecret)).thenReturn(user);
        when(passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())).thenReturn(false);

        InvalidCredentialsException invalidCredentialsException = assertThrows(InvalidCredentialsException.class,
                () -> authService.resetPassword(dto, TOKEN));
        assertThat(invalidCredentialsException.getMessage()).isEqualTo("Invalid credentials provided.");
    }

    @Test
    void changeEmail() {
        when(userClient.existsByEmail(user.getEmail())).thenReturn(false);
        when(cacheManager.isPresent(user.getEmail())).thenReturn(false);

        doNothing().when(mailActionsProducer).executeSendNewEmailVerificationMessage(any(NewEmailVerificationMessage.class));
        doNothing().when(cacheManager).save(eq(user.getEmail()), anyInt(), eq(Duration.ofHours(1)));

        assertThat(authService.changeEmail(user.getEmail())).isEqualTo("The verification code will be sent to the email you specified.");
    }

    @Test
    void changeEmail_userAlreadyExists() {
        when(userClient.existsByEmail(user.getEmail())).thenReturn(true);

        String email = user.getEmail();
        UserAlreadyExistsException userAlreadyExistsException = assertThrows(UserAlreadyExistsException.class,
                () -> authService.changeEmail(email));
        assertThat(userAlreadyExistsException.getMessage()).isEqualTo("User with this email already exists.");
    }

    @Test
    void confirmEmailChange() {
        ConfirmEmailChangeRequest request = getConfirmEmailChangeRequest();

        when(cacheManager.fetch(request.getNewEmail(), Integer.class)).thenReturn(Optional.of(1111));
        when(authenticatedDataProvider.getEmail()).thenReturn(user.getEmail());

        ChangeEmailMessage changeEmailMessage = new ChangeEmailMessage(user.getEmail(), request.getNewEmail());
        doNothing().when(userActionsProducer).executeChangeEmail(changeEmailMessage);

        mockRevokeMethod();
        doNothing().when(cacheManager).delete(request.getNewEmail());

        when(jwtProvider.generateAccessToken(request.getNewEmail())).thenReturn(TOKEN);
        when(refreshTokenGenerator.generate(request.getNewEmail())).thenReturn(TOKEN);

        AuthResponse response = authService.confirmEmailChange(request);
        isValidAuthResponse(response);
    }

    private ConfirmEmailChangeRequest getConfirmEmailChangeRequest() {
        return new ConfirmEmailChangeRequest(1111, "new_" + user.getEmail());
    }

    @Test
    void confirmEmailChange_invalidEmail() {
        ConfirmEmailChangeRequest request = getConfirmEmailChangeRequest();

        when(cacheManager.fetch(request.getNewEmail(), Integer.class)).thenReturn(Optional.empty());

        InvalidEmailException invalidEmailException = assertThrows(InvalidEmailException.class,
                () -> authService.confirmEmailChange(request));
        assertThat(invalidEmailException.getMessage()).isEqualTo("Invalid email.");
    }

    @Test
    void confirmEmailChange_invalidVerificationCode() {
        ConfirmEmailChangeRequest request = getConfirmEmailChangeRequest();

        when(cacheManager.fetch(request.getNewEmail(), Integer.class)).thenReturn(Optional.of(1112));

        InvalidVerificationCodeException invalidVerificationCodeException = assertThrows(InvalidVerificationCodeException.class,
                () -> authService.confirmEmailChange(request));
        assertThat(invalidVerificationCodeException.getMessage()).isEqualTo("Invalid verification code.");
    }
}
