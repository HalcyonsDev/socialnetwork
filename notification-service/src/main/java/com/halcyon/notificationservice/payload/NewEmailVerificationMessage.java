package com.halcyon.notificationservice.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewEmailVerificationMessage {
    private String to;
    private int verificationCode;
}
