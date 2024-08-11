package com.halcyon.authservice.service;

import com.halcyon.authservice.exception.*;
import com.halcyon.authservice.payload.*;
import com.halcyon.clients.user.UserClient;
import com.halcyon.clients.user.UserResponse;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.jwtlibrary.TokenRevocationService;
import com.halcyon.rediscache.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.authservice.security.RefreshTokenGenerator;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

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

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        userActionsProducer.executeCreatingUser(dto);
        AuthResponse response = getAuthResponse(dto.getEmail());

        VerificationMessage verificationMessage = new VerificationMessage(dto.getUsername(), dto.getEmail(), response.getAccessToken());
        mailActionsProducer.executeSendVerificationMessage(verificationMessage);

        return response;
    }

    public AuthResponse login(AuthRequest request) {
        UserResponse user = userClient.getByEmail(request.getEmail(), privateSecret);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        if (user.isUsing2FA()) {
            cacheManager.save("2fa:" + user.getEmail(), Duration.ofMinutes(5));
            return null;
        }

        return getAuthResponse(request.getEmail());
    }

    public String logout() {
        tokenRevocationService.revoke(jwtProvider.extractJti(getToken()));
        return "You have successfully logout from your account.";
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

    public String forgotPassword(String email) {
        UserResponse user = userClient.getByEmail(email, privateSecret);

        String accessToken = jwtProvider.generateAccessToken(email);
        ForgotPasswordMessage forgotPasswordMessage = new ForgotPasswordMessage(user.getEmail(), accessToken);

        mailActionsProducer.executeSendForgotPasswordMessage(forgotPasswordMessage);
        return "A link to reset your password has been sent to your email";
    }

    public String resetPassword(ResetPasswordDto dto, String token) {
        UserResponse user = userClient.getByEmail(jwtProvider.extractEmail(token), privateSecret);

        if (!user.isVerified()) {
            throw new UserIsNotVerifiedException();
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        String newEncodedPassword = passwordEncoder.encode(dto.getNewPassword());
        UserPasswordResetEvent passwordResetEvent = new UserPasswordResetEvent(user.getEmail(), newEncodedPassword);

        userActionsProducer.executeResetPassword(passwordResetEvent);
        tokenRevocationService.revoke(jwtProvider.extractJti(getToken()));

        return "Password has been reset successfully.";
    }

    public String changeEmail(String email) {
        if (userClient.existsByEmail(email) || cacheManager.isPresent(email)) {
            throw new UserAlreadyExistsException();
        }

        int verificationCode = generateVerificationCode();

        NewEmailVerificationMessage verificationMessage = new NewEmailVerificationMessage(email, verificationCode);
        mailActionsProducer.executeSendNewEmailVerificationMessage(verificationMessage);

        cacheManager.save(email, verificationCode, Duration.ofHours(1));

        return "The verification code will be sent to the email you specified.";
    }

    public AuthResponse confirmEmailChange(ConfirmEmailChangeRequest request) {
        int correctVerificationCode = cacheManager.fetch(request.getNewEmail(), Integer.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email."));

        if (request.getVerificationCode() != correctVerificationCode) {
            throw new InvalidVerificationCodeException();
        }

        ChangeEmailMessage changeEmailMessage = new ChangeEmailMessage(authenticatedDataProvider.getEmail(), request.getNewEmail());
        userActionsProducer.executeChangeEmail(changeEmailMessage);

        tokenRevocationService.revoke(jwtProvider.extractJti(getToken()));
        cacheManager.delete(request.getNewEmail());

        return getAuthResponse(request.getNewEmail());
    }

    private AuthResponse getAuthResponse(String email) {
        String accessToken = jwtProvider.generateAccessToken(email);
        String refreshToken = refreshTokenGenerator.generate(email);

        return new AuthResponse(accessToken, refreshToken);
    }

    private int generateVerificationCode() {
        return new Random().nextInt(9999 - 1000 + 1) + 1000;
    }

    private String getToken() {
        return Optional.ofNullable(httpServletRequest.getHeader("Authorization"))
                .orElseThrow(IllegalStateException::new).substring(7);
    }
}
