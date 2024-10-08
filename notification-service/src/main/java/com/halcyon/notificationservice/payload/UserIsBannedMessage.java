package com.halcyon.notificationservice.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIsBannedMessage {
    private String username;
    private String bannedUserEmail;
}
