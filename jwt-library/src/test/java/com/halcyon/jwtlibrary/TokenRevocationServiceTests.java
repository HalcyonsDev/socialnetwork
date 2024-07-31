package com.halcyon.jwtlibrary;

import com.halcyon.rediscache.CacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenRevocationServiceTests {
    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private TokenRevocationService tokenRevocationService;

    @Test
    void revoke() {
        when(jwtProvider.extractJti(anyString())).thenReturn("test_jti");
        when(jwtProvider.extractTimeUntilExpiration(anyString())).thenReturn(Duration.ZERO);

        tokenRevocationService.revoke("test_token");
        verify(cacheManager).save("test_jti", Duration.ZERO);
    }

    @Test
    void isRevoked() {
        when(jwtProvider.extractJti(anyString())).thenReturn("test_jti");
        tokenRevocationService.isRevoked("test_token");

        verify(cacheManager).isPresent("test_jti");
    }
}
