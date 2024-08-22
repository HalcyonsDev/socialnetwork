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

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {
    @Value("${2fa.qr_prefix}")
    private String qr_prefix;

    @Value("${private.secret}")
    private String privateSecret;

    private static final String ISSUER = "SocialNetwork";

    private final AuthenticatedDataProvider authenticatedDataProvider;
    private final UserActionsProducer userActionsProducer;
    private final UserClient userClient;
    private final JwtProvider jwtProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final CacheManager cacheManager;

    public Setup2FAResponse setup() {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        isValidUser(user);

        String secret = Base32.random();
        sendSaveSecretMessage(user.getEmail(), secret);

        return new Setup2FAResponse(generateQrCodeUrl(user.getEmail(), secret));
    }

    private void isValidUser(PrivateUserResponse user) {
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");
    }

    private void sendSaveSecretMessage(String email, String secret) {
        SaveSecretMessage saveSecretMessage = new SaveSecretMessage(email, secret);
        userActionsProducer.executeSaveSecret(saveSecretMessage);
    }

    private String generateQrCodeUrl(String email, String secret) {
        return qr_prefix + URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", ISSUER, email, secret, ISSUER), StandardCharsets.UTF_8) + "&size=200x200";
    }

    public String verify2FA(Verify2FADto dto) {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        verifyOtp(user, dto.getOtp());

        userActionsProducer.executeUse2FA(user.getEmail());

        return "Two-factor authentication is now enabled.";
    }

    private void verifyOtp(PrivateUserResponse user, String otp) {
        Totp totp = new Totp(user.getSecret());

        if (!totp.verify(otp)) {
            throw new InvalidCredentialsException("Invalid verification code (otp)");
        }
    }

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
