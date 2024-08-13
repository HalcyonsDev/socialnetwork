package com.halcyon.clients.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserResponse {
    private long id;
    private String email;
    private String username;
    private String about;
    private String password;
    private String avatarPath;
    private boolean isVerified;
    private boolean isBanned;
    private boolean isUsing2FA;
    private String secret;
    private String authProvider;
}
