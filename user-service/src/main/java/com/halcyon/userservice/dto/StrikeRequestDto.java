package com.halcyon.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrikeRequestDto {
    private String targetEmail;

    @Size(min = 1, max = 100, message = "Cause must be more than 1 character and less than 500 characters.")
    @NotBlank(message = "Cause is required.")
    private String cause;
}
