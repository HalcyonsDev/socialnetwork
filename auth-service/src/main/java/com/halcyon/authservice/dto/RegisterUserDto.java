package com.halcyon.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserDto {
    @Email(message = "Email is not valid.")
    @NotBlank(message = "Email is required.")
    private String email;

    @Size(min = 2, max = 100, message = "Username must be more than 1 character and less than 100 characters.")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9 ]+", message = "Username must start with an alphabet and contain only letters, digits, and spaces.")
    @NotBlank(message = "Username is required.")
    private String username;

    @Size(min = 2, max = 500, message = "\"About me\" must be more than 1 character and less than 500 characters.")
    private String about;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters long.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit.")
    @NotBlank(message = "Password is required")
    private String password;
}
