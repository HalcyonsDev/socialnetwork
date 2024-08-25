package com.halcyon.authservice.service;

import com.halcyon.authservice.exception.OAuth2AuthenticationProcessingException;
import com.halcyon.authservice.security.oauth2.user.OAuth2UserInfo;
import com.halcyon.authservice.security.oauth2.user.OAuth2UserInfoFactory;
import com.halcyon.authservice.security.oauth2.user.UserPrincipal;
import com.halcyon.clients.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * Service class that extends {@link DefaultOAuth2UserService} responsible to handle OAuth2 user authentication
 *
 * @author Ruslan Sadikov
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {
    @Value("${private.secret}")
    private String privateSecret;

    private final UserClient userClient;

    /**
     * Loads the OAuth2 user details retrieved from the OAuth2 provider and processes them.
     *
     * @param userRequest the {@link OAuth2UserRequest} containing the details of the OAuth2 authentication request
     * @return an {@link OAuth2User} representing the authenticated user with any custom processing applied
     * @throws OAuth2AuthenticationException if there is an error during the authentication process
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    /**
     * Processes the OAuth2 user details retrieved from the OAuth2 provider after successful authentication.
     * Retrieves {@link OAuth2UserInfo} information about user and provider {@link OAuth2UserInfoFactory}.
     * Validates user's email address {@link #isValidEmail(String)}.
     * Checks if the user already exists, and either updates the existing user {@link #updateExistingUser(OAuth2UserInfo)}
     * or registers a new one {@link #registerNewUser(OAuth2UserRequest, OAuth2UserInfo)}
     *
     * @param userRequest the {@link OAuth2UserRequest} containing details of the OAuth2 authentication request
     * @param oAuth2User  the {@link OAuth2User} containing user attributes
     * @return {@link OAuth2User} with additional attributes and user information
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                userRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        isValidEmail(oAuth2UserInfo.getEmail());

        PrivateUserResponse user;
        if (userClient.existsByEmail(oAuth2UserInfo.getEmail())) {
            String authProvider = userClient.getByEmail(oAuth2UserInfo.getEmail(), privateSecret).getAuthProvider();
            isCorrectProvider(authProvider, userRequest);

            user = updateExistingUser(oAuth2UserInfo);
        } else {
            user = registerNewUser(userRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    /**
     * Validates the user's email address retrieved from the OAuth2 provider
     *
     * @param email the email address retrieved from the OAuth2 provider to be validated
     * @throws OAuth2AuthenticationProcessingException if the email is empty
     */
    private void isValidEmail(String email) {
        if (ObjectUtils.isEmpty(email)) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }
    }

    /**
     * Verifies that the OAuth2 authentication provider matches the registered provider for the user.
     *
     * @param authProvider the authentication provider retrieved from the user data
     * @param userRequest  the {@link OAuth2UserRequest} containing details of the OAuth2 authentication request
     * @throws OAuth2AuthenticationProcessingException if the authentication provider does not match
     */
    private void isCorrectProvider(String authProvider, OAuth2UserRequest userRequest) {
        if (!userRequest.getClientRegistration().getRegistrationId().equals(authProvider)) {
            throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                    authProvider + " account. Please use your " + authProvider +
                    " account to login.");
        }
    }

    /**
     * Updates the information of an existing user with details retrieved from the OAuth2 provider {@link UserClient}.
     *
     * @param oAuth2UserInfo the {@link OAuth2UserInfo} containing user information retrieved from the OAuth2 provider
     * @return a {@link PrivateUserResponse} with updated user information
     */
    private PrivateUserResponse updateExistingUser(OAuth2UserInfo oAuth2UserInfo) {
        UpdateOAuth2UserDto dto = new UpdateOAuth2UserDto(
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getUsername(),
                oAuth2UserInfo.getAvatarUrl()
        );

        return userClient.updateOAuth2UserData(dto, privateSecret);
    }

    /**
     * Registers a new user with details retrieved from the OAuth2 provider.
     *
     * @param userRequest    the {@link OAuth2UserRequest} containing details of the OAuth2 authentication request
     * @param oAuth2UserInfo the {@link OAuth2UserInfo} containing user information retrieved from the OAuth2 provider
     * @return a {@link PrivateUserResponse} with information of the newly registered user
     */
    private PrivateUserResponse registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        RegisterOAuth2UserDto dto = new RegisterOAuth2UserDto(
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getUsername(),
                oAuth2UserInfo.getAvatarUrl(),
                registrationId
        );

        return userClient.registerOAuth2User(dto, privateSecret);
    }
}
