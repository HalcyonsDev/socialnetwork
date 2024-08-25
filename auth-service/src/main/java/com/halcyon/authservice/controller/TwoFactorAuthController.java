package com.halcyon.authservice.controller;

import com.halcyon.authservice.dto.Login2FADto;
import com.halcyon.authservice.dto.Verify2FADto;
import com.halcyon.authservice.payload.AuthResponse;
import com.halcyon.authservice.payload.Setup2FAResponse;
import com.halcyon.authservice.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing two-factor authentication (2FA) operations.
 * Uses {@link TwoFactorAuthService} to handle the business logic
 *
 * @author Ruslan Sadikov
 */
@RestController
@RequestMapping("/api/v1/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController {
    private final TwoFactorAuthService twoFactorAuthService;

    /**
     * Initiates the setup process for two-factor authentication (2FA) for the current user.
     *
     * @return a {@link ResponseEntity} containing a {@link Setup2FAResponse} with the QR code URL
     */
    @PostMapping("/setup")
    public ResponseEntity<Setup2FAResponse> setup2FA() {
        Setup2FAResponse response = twoFactorAuthService.setup();
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the provided OTP (One-Time Password) to enable two-factor authentication (2FA) for the user.
     *
     * @param dto a {@link Verify2FADto} containing the OTP to be verified
     * @return a {@link ResponseEntity} containing a message indicating that 2FA is enabled
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verify2FA(@RequestBody Verify2FADto dto) {
        String response = twoFactorAuthService.verify2FA(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a user with two-factor authentication (2FA)
     *
     * @param dto a {@link Login2FADto} containing the user's email and OTP for 2FA
     * @return a {@link ResponseEntity} containing an {@link AuthResponse} with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Login2FADto dto) {
        AuthResponse response = twoFactorAuthService.login(dto);
        return ResponseEntity.ok(response);
    }
}
