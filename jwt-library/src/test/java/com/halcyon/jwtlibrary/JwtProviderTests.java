package com.halcyon.jwtlibrary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JwtProviderTests {
    @Mock
    private JwtProvider jwtProvider;


    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        jwtProvider = new JwtProvider("jwt-library", 30, privateKey, publicKey);
    }

    @Test
    void generateAccessToken() {
        String accessToken = jwtProvider.generateAccessToken("test@example.com");
        assertThat(jwtProvider.isValidAccessToken(accessToken)).isTrue();
    }

    @Test
    void extractEmail() {
        String email = "test@example.com";
        String accessToken = jwtProvider.generateAccessToken(email);

        assertThat(jwtProvider.extractEmail(accessToken)).isEqualTo(email);
    }

    @Test
    void extractJti() {
        String accessToken = jwtProvider.generateAccessToken("test@example.com");
        assertThat(jwtProvider.extractJti(accessToken)).isNotNull();
    }

    @Test
    void extractTimeUntilExpiration() {
        String accessToken = jwtProvider.generateAccessToken("test@example.com");
        assertThat(jwtProvider.extractTimeUntilExpiration(accessToken)).isNotNull();
    }
}
