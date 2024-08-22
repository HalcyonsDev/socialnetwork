package com.halcyon.authservice.controller;

import com.halcyon.authservice.payload.ConfirmEmailChangeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.exception.TokenVerificationException;
import com.halcyon.authservice.payload.AuthRequest;
import com.halcyon.authservice.payload.AuthResponse;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.security.RefreshTokenHeaderProvider;
import com.halcyon.authservice.service.AuthService;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RefreshTokenHeaderProvider refreshTokenHeaderProvider;
    private final AuthService authService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterUserDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        AuthResponse response = authService.register(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(Objects.requireNonNullElse(response, "Please, complete two-factor authentication."));

    }

    @DeleteMapping("/logout")
    public ResponseEntity<String> logout() {
        String response = authService.logout();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<String> confirmEmail(@RequestParam("token") String token) {
        String response = authService.confirmByEmail(token);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-email")
    public ResponseEntity<String> changeEmail(@Email @RequestParam("email") String email) {
        String response = authService.changeEmail(email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/confirm-change-email", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> confirmEmailChange(@RequestBody @Valid ConfirmEmailChangeRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        AuthResponse response = authService.confirmEmailChange(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/forgot-password")
    public ResponseEntity<String> forgotPassword() {
        String response = authService.forgotPassword();
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> resetPassword(
            @RequestParam("token") String token,
            @RequestBody @Valid ResetPasswordDto dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        String response = authService.resetPassword(dto, token);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/access", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> getAccessToken() {
        String refreshToken = refreshTokenHeaderProvider.getRefreshToken()
                .orElseThrow(TokenVerificationException::new);
        AuthResponse response = authService.getTokensByRefresh(refreshToken, false);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> getRefreshToken() {
        String refreshToken = refreshTokenHeaderProvider.getRefreshToken()
                .orElseThrow(TokenVerificationException::new);
        AuthResponse response = authService.getTokensByRefresh(refreshToken, true);
        return ResponseEntity.ok(response);
    }
}
