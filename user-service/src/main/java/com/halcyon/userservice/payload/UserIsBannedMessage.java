package com.halcyon.userservice.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserIsBannedMessage {
    private String username;
    private String bannedUserEmail;
}
