package com.halcyon.userservice.payload;

import com.halcyon.userservice.model.User;
import lombok.*;

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

    public PrivateUserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.about =  user.getAbout();
        this.password = user.getPassword();
        this.avatarPath = user.getAvatarPath();
        this.isVerified = user.isVerified();
        this.isBanned = user.isBanned();
        this.isUsing2FA = user.isUsing2FA();
        this.secret = user.getSecret();
        this.authProvider = user.getAuthProvider();
    }
}
