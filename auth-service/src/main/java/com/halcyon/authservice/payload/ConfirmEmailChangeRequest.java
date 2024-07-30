package com.halcyon.authservice.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEmailChangeRequest {
    private int verificationCode;

    @Email(message = "Email is not valid.")
    @NotBlank(message = "Email is required")
    private String newEmail;
}
