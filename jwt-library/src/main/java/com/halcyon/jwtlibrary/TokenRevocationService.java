package com.halcyon.jwtlibrary;

import com.halcyon.rediscache.CacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ComponentScan(basePackages = {
        "com.halcyon.jwtlibrary",
        "com.halcyon.rediscache"
})
public class TokenRevocationService {
    private final JwtProvider jwtProvider;
    private final CacheManager cacheManager;

    public void revoke(String token) {
        String jti = jwtProvider.extractJti(token);
        Duration ttl = jwtProvider.extractTimeUntilExpiration(token);
        cacheManager.save(jti, ttl);
    }

    public boolean isRevoked(String accessToken) {
        String jti = jwtProvider.extractJti(accessToken);
        return cacheManager.isPresent(jti);
    }
}
