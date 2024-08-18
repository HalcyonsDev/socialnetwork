package com.halcyon.clients.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivateUserResponse {
    private long id;
    private String username;
    private String email;
    private String about;
    private String password;
    private String avatarPath;
    private boolean isVerified;
    private boolean isBanned;
    private boolean isUsing2FA;
    private String secret;
    private String authProvider;
}
