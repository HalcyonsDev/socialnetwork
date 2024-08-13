package com.halcyon.authservice.security.oauth2.user;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public abstract String getUsername();
    public abstract String getEmail();
    public abstract String getAvatarUrl();
}
