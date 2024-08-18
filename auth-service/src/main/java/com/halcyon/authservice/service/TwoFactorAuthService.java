package com.halcyon.authservice.service;

import com.halcyon.authservice.dto.Login2FADto;
import com.halcyon.authservice.dto.Verify2FADto;
import com.halcyon.authservice.exception.InvalidCredentialsException;
import com.halcyon.authservice.exception.TwoFactorIsNotRequiredException;
import com.halcyon.authservice.payload.AuthResponse;
import com.halcyon.authservice.payload.SaveSecretMessage;
import com.halcyon.authservice.payload.Setup2FAResponse;
import com.halcyon.authservice.payload.Use2FAMessage;
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
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");

        String secret = Base32.random();

        SaveSecretMessage saveSecretMessage = new SaveSecretMessage(user.getEmail(), secret);
        userActionsProducer.executeSaveSecret(saveSecretMessage);

        return new Setup2FAResponse(generateQrCodeUrl(user.getEmail(), secret));
    }

    public String verify2FA(Verify2FADto dto) {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        Totp totp = new Totp(user.getSecret());

        if (!totp.verify(dto.getOtp())) {
            throw new InvalidCredentialsException("Invalid verification code (otp)");
        }

        Use2FAMessage use2FAMessage = new Use2FAMessage(user.getEmail());
        userActionsProducer.executeUse2FA(use2FAMessage);

        return "Two-factor authentication is now enabled.";
    }

    public AuthResponse login(Login2FADto dto) {
        PrivateUserResponse user = userClient.getByEmail(dto.getEmail(), privateSecret);

        if (!cacheManager.isPresent("2fa:" + user.getEmail())) {
            throw new TwoFactorIsNotRequiredException();
        }

        Totp totp = new Totp(user.getSecret());

        if (!totp.verify(dto.getOtp())) {
            throw new InvalidCredentialsException("Invalid verification code (otp)");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getEmail());
        String refreshToken = refreshTokenGenerator.generate(user.getEmail());

        cacheManager.delete("2fa:" + user.getEmail());

        return new AuthResponse(accessToken, refreshToken);
    }

    private String generateQrCodeUrl(String email, String secret) {
        return qr_prefix + URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", ISSUER, email, secret, ISSUER), StandardCharsets.UTF_8) + "&size=200x200";
    }
}
