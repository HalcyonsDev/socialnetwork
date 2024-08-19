package com.halcyon.authservice.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserPasswordResetMessage {
    private String email;
    private String newEncodedPassword;
}
