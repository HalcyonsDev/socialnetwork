package com.halcyon.jwtlibrary;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class JwtProvider {
    private final String issuer;

    private final int accessTokenValidity;
    private final String privateKey;
    private final String publicKey;

    public JwtProvider(
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token.validity}") int accessTokenValidity,
            @Value("${jwt.access-token.private-key}") String privateKey,
            @Value("${jwt.access-token.public-key}") String publicKey
    ) {
        this.issuer = issuer;
        this.accessTokenValidity = accessTokenValidity;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String generateAccessToken(String email) {
        return generateAccessToken(email, new HashMap<>());
    }

    public String generateAccessToken(String email, Map<String, Boolean> extraClaims) {
        String jti = String.valueOf(UUID.randomUUID());
        Date currentTimestamp = new Date();
        long expiration = TimeUnit.MINUTES.toMillis(accessTokenValidity);
        Date expirationTimestamp = new Date(System.currentTimeMillis() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .id(jti)
                .subject(email)
                .issuer(issuer)
                .issuedAt(currentTimestamp)
                .expiration(expirationTimestamp)
                .signWith(getPrivateKey(), Jwts.SIG.RS512)
                .compact();
    }

    public boolean isValidAccessToken(String accessToken) {
        try {
            Jwts.parser()
                    .verifyWith(getPublicKey())
                    .requireIssuer(issuer)
                    .build()
                    .parse(accessToken);

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public String extractEmail(String accessToken) {
        return extractClaim(accessToken, Claims::getSubject);
    }

    public String extractJti(String accessToken) {
        return extractClaim(accessToken, Claims::getId);
    }

    public Duration extractTimeUntilExpiration(String accessToken) {
        Instant expirationTimestamp = extractClaim(accessToken, Claims::getExpiration).toInstant();
        Instant currentTimestamp = new Date().toInstant();
        return Duration.between(currentTimestamp, expirationTimestamp);
    }

    private <T> T extractClaim(String accessToken, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        return claimsResolver.apply(claims);
    }

    private PrivateKey getPrivateKey() {
        String sanitizedPrivateKey = sanitizeKey(privateKey);

        byte[] decodedPrivateKey = Decoders.BASE64.decode(sanitizedPrivateKey);

        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodedPrivateKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new InvalidKeyException("Failed to generate private key", ex);
        }
    }

    private PublicKey getPublicKey() {
        String sanitizedPublicKey = sanitizeKey(publicKey);

        byte[] decodedPublicKey = Decoders.BASE64.decode(sanitizedPublicKey);

        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodedPublicKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new InvalidKeyException("Failed to generate public key", ex);
        }
    }

    private String sanitizeKey(String key) {
        return key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\s", "");
    }
}
