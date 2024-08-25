package com.halcyon.authservice.controller;

import com.halcyon.authservice.payload.ConfirmEmailChangeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/**
 * REST controller that handles authentication and authorization-related operations.
 * Uses {@link AuthService} to handle the business logic
 * and {@link RefreshTokenHeaderProvider} to get refresh token from header
 *
 * @author Ruslan Sadikov
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RefreshTokenHeaderProvider refreshTokenHeaderProvider;
    private final AuthService authService;

    /**
     * Registers a new user with the provided registration details
     *
     * @param dto           the {@link RegisterUserDto} data transfer object containing the user's registration details
     * @param bindingResult the {@link BindingResult} containing validation errors, if any
     * @return a {@link ResponseEntity} containing the {@link AuthResponse} with authentication tokens
     * @throws ResponseStatusException if the validation fails and there are errors in the {@link BindingResult}
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterUserDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        AuthResponse response = authService.register(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a user based on the provided credentials
     *
     * @param request the {@link AuthRequest} containing the user's login credentials
     * @return a {@link ResponseEntity} containing the {@link AuthResponse} with authentication tokens
     * or a message prompting the user to complete 2FA if applicable
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(Objects.requireNonNullElse(response, "Please, complete two-factor authentication."));
    }

    /**
     * Logs out the authenticated user
     *
     * @return a {@link ResponseEntity} containing a message indicating that the user has successfully logged out
     */
    @DeleteMapping("/logout")
    public ResponseEntity<String> logout() {
        String response = authService.logout();
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a new access token using the provided refresh token
     * which was retrieved from header {@link RefreshTokenHeaderProvider}
     *
     * @return a {@link ResponseEntity} containing {@link AuthResponse} with the new access token
     */
    @PutMapping("/access")
    public ResponseEntity<AuthResponse> getAccessToken() {
        String refreshToken = refreshTokenHeaderProvider.getRefreshToken()
                .orElseThrow(TokenVerificationException::new);
        AuthResponse response = authService.getTokensByRefresh(refreshToken, false);
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a new access and refresh tokens using the provided refresh token
     * which was retrieved from header {@link RefreshTokenHeaderProvider}
     *
     * @return a {@link ResponseEntity} containing {@link AuthResponse} with the new authentication tokens
     */
    @PutMapping("/refresh")
    public ResponseEntity<AuthResponse> getRefreshToken() {
        String refreshToken = refreshTokenHeaderProvider.getRefreshToken()
                .orElseThrow(TokenVerificationException::new);
        AuthResponse response = authService.getTokensByRefresh(refreshToken, true);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms a user's email address using the provided token.
     *
     * @param token the user's email confirmation token
     * @return a {@link ResponseEntity} containing a confirmation message indicating that the email has been verified
     */
    @GetMapping
    public ResponseEntity<String> confirmEmail(@RequestParam("token") String token) {
        String response = authService.confirmByEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates the process to change the user's email address
     *
     * @param email the new email address to be associated with the user's account
     * @return a {@link ResponseEntity} containing a message indicating that the verification code has been sent to the new email address
     */
    @PatchMapping("/change-email")
    public ResponseEntity<String> changeEmail(@Email @RequestParam("email") String email) {
        String response = authService.changeEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms the user's email change request using the verification code
     *
     * @param request       the {@link ConfirmEmailChangeRequest} containing the new email and verification code.
     * @param bindingResult the {@link BindingResult} containing validation errors, if any
     * @return a {@link ResponseEntity} containing the {@link AuthResponse} with new authentication tokens
     * @throws ResponseStatusException if the validation fails and there are errors in the {@link BindingResult}
     */
    @PatchMapping("/confirm-change-email")
    public ResponseEntity<AuthResponse> confirmEmailChange(@RequestBody @Valid ConfirmEmailChangeRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        AuthResponse response = authService.confirmEmailChange(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates the password reset process by sending a reset link to the user's email.
     *
     * @return a {@link ResponseEntity} containing a message that a reset link has been sent to the user's email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword() {
        String response = authService.forgotPassword();
        return ResponseEntity.ok(response);
    }

    /**
     * Resets the user's password using the provided token and new password
     *
     * @param token         the password reset user's token
     * @param dto           the {@link ResetPasswordDto} containing the current and new passwords
     * @param bindingResult the {@link BindingResult} containing validation errors, if any
     * @return a {@link ResponseEntity} containing a message indicating that the password has been reset successfully
     * @throws ResponseStatusException if the validation fails and there are errors in the {@link BindingResult}
     */
    @PostMapping("/reset-password")
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
}
