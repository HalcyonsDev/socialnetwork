package com.halcyon.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOAuth2UserDto {
    private String email;
    private String username;
    private String avatarUrl;
}
