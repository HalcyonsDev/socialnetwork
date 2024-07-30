package com.halcyon.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class TokenConfigProperties {
    private RefreshToken refreshToken = new RefreshToken();

    @Getter
    @Setter
    public static class RefreshToken {
        private Integer validity;
    }
}
