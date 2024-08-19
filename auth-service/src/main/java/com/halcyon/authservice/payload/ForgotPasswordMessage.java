package com.halcyon.authservice.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ForgotPasswordMessage {
    private String email;
    private String token;
}
