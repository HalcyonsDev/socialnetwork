package com.halcyon.authservice.security.oauth2.user;

import com.halcyon.clients.user.PrivateUserResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@Setter
public class UserPrincipal implements OAuth2User {
    private long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(long id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserPrincipal create(PrivateUserResponse user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), Collections.emptyList());
    }

    public static UserPrincipal create(PrivateUserResponse user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = create(user);
        userPrincipal.setAttributes(attributes);

        return userPrincipal;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
