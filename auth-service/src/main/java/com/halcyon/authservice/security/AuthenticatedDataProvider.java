package com.halcyon.authservice.security;

import com.halcyon.jwtlibrary.JwtAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedDataProvider {
    public String getEmail() {
        return Optional.ofNullable((JwtAuthentication) SecurityContextHolder.getContext().getAuthentication())
                .map(JwtAuthentication::getEmail)
                .orElseThrow(IllegalStateException::new);
    }
}
