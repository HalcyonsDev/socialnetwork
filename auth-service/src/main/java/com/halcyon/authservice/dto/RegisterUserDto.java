package com.halcyon.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserDto {
    @Email(message = "Email is not valid.")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 1, max = 100, message = "Username must be more than 1 character and less than 100 characters.")
    @Pattern(regexp = "[a-zA-Z0-9-]+", message = "Username must contain only letters, digits, and dashes")
    @NotBlank(message = "Username is required")
    private String username;

    @Size(min = 1, max = 100, message = "\"About me\" must be more than 1 character and less than 500 characters.")
    private String about;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    @NotBlank(message = "Password is required")
    private String password;
}