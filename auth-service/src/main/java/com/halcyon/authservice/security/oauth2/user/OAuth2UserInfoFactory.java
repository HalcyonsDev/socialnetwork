package com.halcyon.authservice.security.oauth2.user;

import com.halcyon.authservice.exception.OAuth2AuthenticationProcessingException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("github")) {
            return new GithubOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("discord")) {
            return new DiscordOAuth2UserInfo(attributes);
        }

        throw new OAuth2AuthenticationProcessingException("Sorry, login with " + registrationId + " is not supported yet.");
    }
}
