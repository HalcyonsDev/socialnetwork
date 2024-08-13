package com.halcyon.clients.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOAuth2UserDto {
    private String email;
    private String username;
    private String avatarUrl;
    private String authProvider;
}
