package com.halcyon.authservice.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VerificationMessage {
    private String username;
    private String to;
    private String token;
}
