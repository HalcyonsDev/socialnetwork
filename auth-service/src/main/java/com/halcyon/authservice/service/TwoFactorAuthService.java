package com.halcyon.authservice.service;

import com.halcyon.authservice.dto.Login2FADto;
import com.halcyon.authservice.dto.Verify2FADto;
import com.halcyon.authservice.exception.InvalidCredentialsException;
import com.halcyon.authservice.exception.TwoFactorIsNotRequiredException;
import com.halcyon.authservice.payload.AuthResponse;
import com.halcyon.authservice.payload.SaveSecretMessage;
import com.halcyon.authservice.payload.Setup2FAResponse;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.authservice.security.RefreshTokenGenerator;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.rediscache.CacheManager;
import lombok.RequiredArgsConstructor;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

/**
 * Service class responsible for managing two-factor authentication (2FA) processes for users
 *
 * @author Ruslan Sadikov
 */
@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {
    @Value("${2fa.qr_prefix}")
    private String qrPrefix;

    @Value("${private.secret}")
    private String privateSecret;

    private static final String ISSUER = "SocialNetwork";

    private final AuthenticatedDataProvider authenticatedDataProvider;
    private final UserActionsProducer userActionsProducer;
    private final UserClient userClient;
    private final JwtProvider jwtProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final CacheManager cacheManager;

    /**
     * Initiates the setup two-factor authentication (2FA) process for the current user.
     * Validates the user's status {@link #isValidUser(PrivateUserResponse)}.
     * Generates secret, qrcode {@link #generateQrCodeUrl(String, String)}.
     * Sends save secret message {@link #sendSaveSecretMessage(String, String)}.
     *
     * @return a {@link Setup2FAResponse} containing the QR code URL for setting up 2FA
     * @throws com.halcyon.clients.exception.BannedUserException if the user is banned
     * @throws com.halcyon.clients.exception.UnverifiedUserException if the user is not verified
     */
    public Setup2FAResponse setup() {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        isValidUser(user);

        String secret = Base32.random();
        sendSaveSecretMessage(user.getEmail(), secret);

        return new Setup2FAResponse(generateQrCodeUrl(user.getEmail(), secret));
    }

    /**
     * Validates the user's status by checking whether the user is banned or not verified {@link com.halcyon.clients.util.UserUtil}.
     *
     * @param user the {@link PrivateUserResponse} representing the user to be validated
     * @throws com.halcyon.clients.exception.BannedUserException if the user is banned
     * @throws com.halcyon.clients.exception.UnverifiedUserException if the user is not verified
     */
    private void isValidUser(PrivateUserResponse user) {
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");
    }

    /**
     * Sends a message {@link SaveSecretMessage} to save the generated 2FA secret key in the user-service {@link UserActionsProducer}.
     *
     * @param email  the email address of the user for whom the 2FA secret is being saved
     * @param secret secret the 2FA secret key to be saved
     */
    private void sendSaveSecretMessage(String email, String secret) {
        SaveSecretMessage saveSecretMessage = new SaveSecretMessage(email, secret);
        userActionsProducer.executeSaveSecret(saveSecretMessage);
    }

    /**
     * Generates a URL for a QR code that can be scanned to set up two-factor authentication (2FA).
     *
     * @param email  the user's email address used as the account name in the QR code URL
     * @param secret the 2FA secret key associated with the user used for generating OTPs
     * @return URL for the QR code that users can scan to configure 2FA
     */
    private String generateQrCodeUrl(String email, String secret) {
        return qrPrefix + URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", ISSUER, email, secret, ISSUER), StandardCharsets.UTF_8) + "&size=200x200";
    }

    /**
     * Verifies the provided OTP (One-Time Password) {@link #verifyOtp(PrivateUserResponse, String)}.
     * and enables two-factor authentication (2FA) for the user.
     * Sends message to update the user's status to indicate that 2FA is enabled in user-service {@link UserActionsProducer}
     *
     * @param dto a {@link Verify2FADto} containing the OTP to be verified
     * @return a message indicating that two-factor authentication (2FA) is now enabled
     */
    public String verify2FA(Verify2FADto dto) {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        verifyOtp(user, dto.getOtp());

        userActionsProducer.executeUse2FA(user.getEmail());

        return "Two-factor authentication is now enabled.";
    }

    /**
     * Verifies the provided OTP (One-Time Password) using {@link Totp}.
     *
     * @param user the {@link PrivateUserResponse} representing the user, which includes the 2FA secret
     * @param otp  the OTP to be verified
     * @throws InvalidCredentialsException if the provided OTP is invalid for current user
     */
    private void verifyOtp(PrivateUserResponse user, String otp) {
        Totp totp = new Totp(user.getSecret());

        if (!totp.verify(otp)) {
            throw new InvalidCredentialsException("Invalid verification code (otp)");
        }
    }

    /**
     * Authenticates a user with two-factor authentication (2FA).
     * Checks if 2FA is required by verifying the presence of a 2FA-related entry in the cache {@link CacheManager}.
     * Verifies OTP (One-Time Password) {@link #verifyOtp(PrivateUserResponse, String)}.
     * Generates new access token {@link JwtProvider} and new refresh token {@link RefreshTokenGenerator}.
     * Deletes cache entry upon successful authentication {@link CacheManager}
     *
     * @param dto a {@link Login2FADto} containing the user's email and OTP for 2FA
     * @return an {@link AuthResponse} containing new authentication tokens if authentication is successful
     * @throws TwoFactorIsNotRequiredException if 2FA is not required or not found for the user
     * @throws InvalidCredentialsException if the provided OTP is invalid
     */
    public AuthResponse login(Login2FADto dto) {
        PrivateUserResponse user = userClient.getByEmail(dto.getEmail(), privateSecret);

        if (!cacheManager.isPresent("2fa:" + user.getEmail())) {
            throw new TwoFactorIsNotRequiredException();
        }

        verifyOtp(user, dto.getOtp());

        String accessToken = jwtProvider.generateAccessToken(user.getEmail());
        String refreshToken = refreshTokenGenerator.generate(user.getEmail());

        cacheManager.delete("2fa:" + user.getEmail());

        return new AuthResponse(accessToken, refreshToken);
    }
}
