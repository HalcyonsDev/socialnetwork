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

@RestController
@RequestMapping("/api/v1/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController {
    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/setup")
    public ResponseEntity<Setup2FAResponse> setup2FA() {
        Setup2FAResponse response = twoFactorAuthService.setup();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify2FA(@RequestBody Verify2FADto dto) {
        String response = twoFactorAuthService.verify2FA(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Login2FADto dto) {
        AuthResponse response = twoFactorAuthService.login(dto);
        return ResponseEntity.ok(response);
    }
}
