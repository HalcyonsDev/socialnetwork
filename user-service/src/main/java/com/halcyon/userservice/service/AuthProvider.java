package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.JwtAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthProvider {
    public String getSubject() {
        return getAuthInfo().getEmail();
    }

    private JwtAuthentication getAuthInfo() {
        return (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }
}
