package com.halcyon.authservice.security.oauth2.user;

import java.util.Map;

public class GithubOAuth2UserInfo extends OAuth2UserInfo {
    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }


    @Override
    public String getUsername() {
        return attributes.get("name").toString();
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }

    @Override
    public String getAvatarUrl() {
        return attributes.get("avatar_url").toString();
    }
}