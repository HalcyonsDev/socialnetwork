package com.halcyon.authservice.service;

import com.halcyon.authservice.exception.*;
import com.halcyon.authservice.payload.*;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.jwtlibrary.TokenRevocationService;
import com.halcyon.rediscache.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.authservice.security.RefreshTokenGenerator;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Value("${private.secret}")
    private String privateSecret;

    private final JwtProvider jwtProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenRevocationService tokenRevocationService;
    private final AuthenticatedDataProvider authenticatedDataProvider;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;
    private final UserActionsProducer userActionsProducer;
    private final MailActionsProducer mailActionsProducer;
    private final HttpServletRequest httpServletRequest;

    public AuthResponse register(RegisterUserDto dto) {
        if (userClient.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException();
        }

        sendCreatingUserMessage(dto);
        AuthResponse response = getAuthResponse(dto.getEmail());
        sendVerificationMailMessage(dto.getUsername(), dto.getEmail(), response.getAccessToken());

        return response;
    }

    private void sendCreatingUserMessage(RegisterUserDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        userActionsProducer.executeCreatingUser(dto);
    }

    private void sendVerificationMailMessage(String username, String email, String accessToken) {
        VerificationMessage verificationMessage = new VerificationMessage(username, email, accessToken);
        mailActionsProducer.executeSendVerificationMessage(verificationMessage);
    }

    public AuthResponse login(AuthRequest request) {
        PrivateUserResponse user = userClient.getByEmail(request.getEmail(), privateSecret);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        if (user.isUsing2FA()) {
            cacheManager.save("2fa:" + user.getEmail(), Duration.ofMinutes(5));
            return null;
        }

        return getAuthResponse(request.getEmail());
    }

    private AuthResponse getAuthResponse(String email) {
        String accessToken = jwtProvider.generateAccessToken(email);
        String refreshToken = refreshTokenGenerator.generate(email);

        return new AuthResponse(accessToken, refreshToken);
    }

    public String logout() {
        tokenRevocationService.revoke(getToken());
        return "You have successfully logout from your account.";
    }

    private String getToken() {
        return Optional.ofNullable(httpServletRequest.getHeader("Authorization"))
                .orElseThrow(IllegalStateException::new).substring(7);
    }

    public String confirmByEmail(String accessToken) {
        String subject = jwtProvider.extractEmail(accessToken);
        userActionsProducer.executeConfirmByEmail(subject);

        return "Account is verified.";
    }

    public AuthResponse getTokensByRefresh(String refreshToken, boolean isRefresh) {
        String subject = cacheManager.fetch(refreshToken, String.class)
                .orElseThrow(TokenVerificationException::new);

        String accessToken = jwtProvider.generateAccessToken(subject);
        String newRefreshToken = isRefresh ? refreshTokenGenerator.generate(subject) : null;

        return new AuthResponse(accessToken, newRefreshToken);
    }

    public String forgotPassword() {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        sendForgotPasswordMailMessage(user.getEmail());

        return "A link to reset your password has been sent to your email.";
    }

    private void sendForgotPasswordMailMessage(String email) {
        String accessToken = jwtProvider.generateAccessToken(email);
        ForgotPasswordMessage forgotPasswordMessage = new ForgotPasswordMessage(email, accessToken);
        mailActionsProducer.executeSendForgotPasswordMessage(forgotPasswordMessage);
    }

    public String resetPassword(ResetPasswordDto dto, String token) {
        PrivateUserResponse user = userClient.getByEmail(jwtProvider.extractEmail(token), privateSecret);
        isValidUser(user);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        sendUserPasswordResetMessage(user.getEmail(), dto.getNewPassword());
        tokenRevocationService.revoke(token);

        return "Password has been reset successfully.";
    }

    private void sendUserPasswordResetMessage(String email, String newPassword) {
        String newEncodedPassword = passwordEncoder.encode(newPassword);
        UserPasswordResetMessage userPasswordResetMessage = new UserPasswordResetMessage(email, newEncodedPassword);
        userActionsProducer.executeResetPassword(userPasswordResetMessage);
    }

    private void isValidUser(PrivateUserResponse user) {
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");
    }

    public String changeEmail(String email) {
        if (userClient.existsByEmail(email) || cacheManager.isPresent(email)) {
            throw new UserAlreadyExistsException();
        }

        int verificationCode = generateVerificationCode();
        sendEmailVerificationMailMessage(email, verificationCode);

        cacheManager.save(email, verificationCode, Duration.ofHours(1));

        return "The verification code will be sent to the email you specified.";
    }

    private int generateVerificationCode() {
        return new Random().nextInt(9999 - 1000 + 1) + 1000;
    }

    private void sendEmailVerificationMailMessage(String email, int verificationCode) {
        NewEmailVerificationMessage verificationMessage = new NewEmailVerificationMessage(email, verificationCode);
        mailActionsProducer.executeSendNewEmailVerificationMessage(verificationMessage);
    }

    public AuthResponse confirmEmailChange(ConfirmEmailChangeRequest request) {
        int correctVerificationCode = cacheManager.fetch(request.getNewEmail(), Integer.class)
                .orElseThrow(InvalidEmailException::new);

        if (request.getVerificationCode() != correctVerificationCode) {
            throw new InvalidVerificationCodeException();
        }

        sendChangeEmailMessage(authenticatedDataProvider.getEmail(), request.getNewEmail());

        tokenRevocationService.revoke(getToken());
        cacheManager.delete(request.getNewEmail());

        return getAuthResponse(request.getNewEmail());
    }

    private void sendChangeEmailMessage(String currentEmail, String newEmail) {
        ChangeEmailMessage changeEmailMessage = new ChangeEmailMessage(currentEmail, newEmail);
        userActionsProducer.executeChangeEmail(changeEmailMessage);
    }
}
