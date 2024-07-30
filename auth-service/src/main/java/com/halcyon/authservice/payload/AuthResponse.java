package com.halcyon.authservice.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private final String TYPE = "Bearer";
    private final String accessToken;
    private final String refreshToken;
}
