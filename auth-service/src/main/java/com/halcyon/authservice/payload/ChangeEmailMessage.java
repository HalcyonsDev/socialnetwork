package com.halcyon.authservice.payload;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChangeEmailMessage {
    private String currentEmail;
    private String newEmail;
}
