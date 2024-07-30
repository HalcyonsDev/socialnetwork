package com.halcyon.authservice.security;

import com.halcyon.rediscache.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import com.halcyon.authservice.config.TokenConfigProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(TokenConfigProperties.class)
public class RefreshTokenGenerator {
    private final CacheManager cacheManager;
    private final TokenConfigProperties tokenConfigProperties;

    private static final String ALGORITHM = "SHA256";

    @SneakyThrows
    public String generate(String email) {
        String randomIdentifier = String.valueOf(UUID.randomUUID());
        MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
        byte[] hash = messageDigest.digest(randomIdentifier.getBytes(StandardCharsets.UTF_8));

        String refreshToken = convertBytesToString(hash);
        saveRefreshTokenInCache(refreshToken, email);

        return refreshToken;
    }

    private String convertBytesToString(byte[] bytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte currentByte: bytes) {
            String hexValue = String.format("%02x", currentByte);
            hexStringBuilder.append(hexValue);
        }

        return hexStringBuilder.toString();
    }

    private void saveRefreshTokenInCache(String refreshToken, String email) {
        int refreshTokenValidity = tokenConfigProperties.getRefreshToken().getValidity();
        cacheManager.save(refreshToken, email, Duration.ofMinutes(refreshTokenValidity));
    }
}
