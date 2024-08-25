package com.halcyon.authservice.service;

import com.halcyon.authservice.exception.*;
import com.halcyon.authservice.payload.*;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.JwtProvider;
import com.halcyon.jwtlibrary.TokenRevocationService;
import com.halcyon.rediscache.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.halcyon.authservice.dto.RegisterUserDto;
import com.halcyon.authservice.dto.ResetPasswordDto;
import com.halcyon.authservice.security.AuthenticatedDataProvider;
import com.halcyon.authservice.security.RefreshTokenGenerator;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

/**
 * Service class responsible for user authentication and authorization processes
 *
 * @author Ruslan Sadikov
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    @Value("${private.secret}")
    private String privateSecret;

    private final JwtProvider jwtProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenRevocationService tokenRevocationService;
    private final AuthenticatedDataProvider authenticatedDataProvider;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;
    private final UserActionsProducer userActionsProducer;
    private final MailActionsProducer mailActionsProducer;
    private final HttpServletRequest httpServletRequest;

    private final Random random = new Random();

    /**
     * Registers a new user by sending user creation {@link #sendCreatingUserMessage(RegisterUserDto)}
     * and verification mail messages {@link #sendVerificationMailMessage(String, String, String)}
     *
     * @param dto the {@link RegisterUserDto} data transfer object containing user registration data
     * @return an {@link AuthResponse} containing jwt access and refresh tokens
     * @throws UserAlreadyExistsException if a user with the provided email already exists {@link UserClient}
     */
    public AuthResponse register(RegisterUserDto dto) {
        if (userClient.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException();
        }

        sendCreatingUserMessage(dto);
        AuthResponse response = getAuthResponse(dto.getEmail());
        sendVerificationMailMessage(dto.getUsername(), dto.getEmail(), response.getAccessToken());

        return response;
    }

    /**
     * Encodes the user's password and sends a message {@link RegisterUserDto}
     * to user-service to create a new user {@link UserActionsProducer}
     *
     * @param dto the {@link RegisterUserDto} data transfer object containing user registration data
     */
    private void sendCreatingUserMessage(RegisterUserDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        userActionsProducer.executeCreatingUser(dto);
    }

    /**
     * Sends a verification email message {@link VerificationMessage} to notification-service {@link MailActionsProducer}
     *
     * @param username    the username of the newly registered user
     * @param email       the email address of the newly registered user
     * @param accessToken the jwt access token
     */
    private void sendVerificationMailMessage(String username, String email, String accessToken) {
        VerificationMessage verificationMessage = new VerificationMessage(username, email, accessToken);
        mailActionsProducer.executeSendVerificationMessage(verificationMessage);
    }

    /**
     * Create an {@link AuthResponse} for the specified user
     * by generating jwt access token {@link JwtProvider} and refresh token {@link RefreshTokenGenerator}
     *
     * @param email the email address of the user for whom the tokens are being generated
     * @return an {@link AuthResponse} containing the generated jwt access and refresh tokens
     */
    private AuthResponse getAuthResponse(String email) {
        String accessToken = jwtProvider.generateAccessToken(email);
        String refreshToken = refreshTokenGenerator.generate(email);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Authenticates a user based on the provided credentials and returns an {@link AuthResponse} if successful
     * or if the user is using two-factor authentication (2FA) {@link TwoFactorAuthService}
     * caches {@link CacheManager} the 2FA requirement and returns null
     *
     * @param request the {@link AuthRequest} containing the email and password provided by user
     * @return an {@link AuthResponse} containing the generated jwt access and refresh tokens
     * @throws InvalidCredentialsException if the provided credentials are invalid
     */
    public AuthResponse login(AuthRequest request) {
        PrivateUserResponse user = userClient.getByEmail(request.getEmail(), privateSecret);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        if (user.isUsing2FA()) {
            cacheManager.save("2fa:" + user.getEmail(), Duration.ofMinutes(5));
            return null;
        }

        return getAuthResponse(request.getEmail());
    }

    /**
     * Logs out the current user by revoking his authentication token {@link TokenRevocationService}
     *
     * @return a confirmation message indicating successful logout
     */
    public String logout() {
        tokenRevocationService.revoke(getToken());
        return "You have successfully logout from your account.";
    }

    /**
     * Retrieves the authentication token from the HTTP request authorization header
     *
     * @return the extracted authentication token
     * @throws TokenNotFoundException if the authorization header was missed
     */
    private String getToken() {
        return Optional.ofNullable(httpServletRequest.getHeader("Authorization"))
                .orElseThrow(TokenNotFoundException::new).substring(7);
    }

    /**
     * Confirms a user's account using the provided access token
     * Sends message to confirm email for user-service {@link UserActionsProducer}
     *
     * @param accessToken the jwt access token used to extract the user's email {@link JwtProvider}
     * @return a confirmation message indicating that the account has been successfully verified
     */
    public String confirmByEmail(String accessToken) {
        String subject = jwtProvider.extractEmail(accessToken);
        userActionsProducer.executeConfirmByEmail(subject);

        return "Account is verified.";
    }

    /**
     * Retrieves a new access token {@link JwtProvider} and optionally
     * a new refresh token {@link RefreshTokenGenerator} based on the provided refresh token
     *
     * @param refreshToken the refresh token used to fetch the subject from cache {@link CacheManager} and generate new tokens
     * @param isRefresh flag indicating whether a new refresh token should be generated
     * @return an {@link AuthResponse} containing the newly generated access token and optionally a new refresh token
     * @throws TokenVerificationException if the provided refresh token is invalid or not found in the cache
     */
    public AuthResponse getTokensByRefresh(String refreshToken, boolean isRefresh) {
        String subject = cacheManager.fetch(refreshToken, String.class)
                .orElseThrow(TokenVerificationException::new);

        String accessToken = jwtProvider.generateAccessToken(subject);
        String newRefreshToken = isRefresh ? refreshTokenGenerator.generate(subject) : null;

        return new AuthResponse(accessToken, newRefreshToken);
    }

    /**
     * Initiates the password forget process for the currently authenticated user
     * by sending mail message to reset password {@link #sendForgotPasswordMailMessage(String)}
     *
     * @return a message indicating that a password reset link has been sent to the user's email address
     */
    public String forgotPassword() {
        PrivateUserResponse user = userClient.getByEmail(authenticatedDataProvider.getEmail(), privateSecret);
        sendForgotPasswordMailMessage(user.getEmail());

        return "A link to reset your password has been sent to your email.";
    }

    /**
     * Sends a forgot password mail message {@link ForgotPasswordMessage}
     * to the specified email address in notification-service {@link MailActionsProducer}
     *
     * @param email the email address to which the message will be sent
     */
    private void sendForgotPasswordMailMessage(String email) {
        String accessToken = jwtProvider.generateAccessToken(email);
        ForgotPasswordMessage forgotPasswordMessage = new ForgotPasswordMessage(email, accessToken);
        mailActionsProducer.executeSendForgotPasswordMessage(forgotPasswordMessage);
    }

    /**
     * Resets the user's password after verifying the provided token and current password
     * Validates the user {@link #isValidUser(PrivateUserResponse)}
     * Sends password reset message {@link #sendUserPasswordResetMessage(String, String)}
     * Revokes the authentication token {@link TokenRevocationService}
     *
     * @param dto the {@link ResetPasswordDto} data transfer object containing the user's current and new passwords
     * @param token the token provided to authorize the password reset
     * @return a message indicating that the password has been reset successfully
     * @throws InvalidCredentialsException if the current password does not match the user's existing password
     */
    public String resetPassword(ResetPasswordDto dto, String token) {
        PrivateUserResponse user = userClient.getByEmail(jwtProvider.extractEmail(token), privateSecret);
        isValidUser(user);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials provided.");
        }

        sendUserPasswordResetMessage(user.getEmail(), dto.getNewPassword());
        tokenRevocationService.revoke(token);

        return "Password has been reset successfully.";
    }

    /**
     * Encodes the user's new password
     * Sends password reset message {@link UserPasswordResetMessage} to user-service {@link UserActionsProducer}
     *
     * @param email the email address of the user whom password is being reset
     * @param newPassword the new password to be set for the user
     */
    private void sendUserPasswordResetMessage(String email, String newPassword) {
        String newEncodedPassword = passwordEncoder.encode(newPassword);
        UserPasswordResetMessage userPasswordResetMessage = new UserPasswordResetMessage(email, newEncodedPassword);
        userActionsProducer.executeResetPassword(userPasswordResetMessage);
    }

    /**
     * Validates the user's status by checking whether the user is banned or not verified {@link com.halcyon.clients.util.UserUtil}
     *
     * @param user the {@link PrivateUserResponse} representing the user to be validated
     * @throws com.halcyon.clients.exception.BannedUserException if the user is banned
     * @throws com.halcyon.clients.exception.UnverifiedUserException if the user is not verified
     */
    private void isValidUser(PrivateUserResponse user) {
        isUserBanned(user, "You are banned.");
        isUserVerified(user, "You are not verified. Please confirm your email.");
    }

    /**
     * Initiates the process of changing the user's email address
     * by generating {@link #generateVerificationCode()} and sending verification code message {@link #sendEmailVerificationMailMessage(String, int)}
     * Saves email and code in cache {@link CacheManager}
     *
     * @param email the new email address to which the verification code will be sent
     * @return a message indicating that the verification code has been sent to the specified email
     * @throws UserAlreadyExistsException if the provided email is already in use
     */
    public String changeEmail(String email) {
        if (userClient.existsByEmail(email) || cacheManager.isPresent(email)) {
            throw new UserAlreadyExistsException();
        }

        int verificationCode = generateVerificationCode();
        sendEmailVerificationMailMessage(email, verificationCode);

        cacheManager.save(email, verificationCode, Duration.ofHours(1));

        return "The verification code will be sent to the email you specified.";
    }

    /**
     * Generates a random 4-digit verification code
     *
     * @return a 4-digit verification code
     */
    private int generateVerificationCode() {
        return random.nextInt(9999 - 1000 + 1) + 1000;
    }

    /**
     * Sends a new email verification message {@link NewEmailVerificationMessage} to notification-service {@link MailActionsProducer}
     *
     * @param email            the email address to which the verification code will be sent
     * @param verificationCode the verification code to be sent to the specified email
     */
    private void sendEmailVerificationMailMessage(String email, int verificationCode) {
        NewEmailVerificationMessage verificationMessage = new NewEmailVerificationMessage(email, verificationCode);
        mailActionsProducer.executeSendNewEmailVerificationMessage(verificationMessage);
    }

    /**
     * Confirms the email change process by getting from cache {@link CacheManager} and verifying the provided verification code
     * Sends change email message {@link #sendChangeEmailMessage(String, String)} and revokes token {@link TokenRevocationService}
     * Deletes email and code from cache {@link CacheManager}
     *
     * @param request the {@link ConfirmEmailChangeRequest} containing the new email address and verification code
     * @return an {@link AuthResponse} containing the new generated access and refresh tokens
     * @throws InvalidEmailException if the email is not found in cache
     * @throws InvalidVerificationCodeException if the provided verification code is incorrect
     */
    public AuthResponse confirmEmailChange(ConfirmEmailChangeRequest request) {
        int correctVerificationCode = cacheManager.fetch(request.getNewEmail(), Integer.class)
                .orElseThrow(InvalidEmailException::new);

        if (request.getVerificationCode() != correctVerificationCode) {
            throw new InvalidVerificationCodeException();
        }

        sendChangeEmailMessage(authenticatedDataProvider.getEmail(), request.getNewEmail());

        tokenRevocationService.revoke(getToken());
        cacheManager.delete(request.getNewEmail());

        return getAuthResponse(request.getNewEmail());
    }

    /**
     * Sends a change email message {@link ChangeEmailMessage} to update
     * the user's email address in user-service {@link UserActionsProducer}
     *
     * @param currentEmail the user's current email message
     * @param newEmail the new email address to be set for the user
     */
    private void sendChangeEmailMessage(String currentEmail, String newEmail) {
        ChangeEmailMessage changeEmailMessage = new ChangeEmailMessage(currentEmail, newEmail);
        userActionsProducer.executeChangeEmail(changeEmailMessage);
    }
}
