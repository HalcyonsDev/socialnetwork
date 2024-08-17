package com.halcyon.chatservice.dto;

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
public class UpdateMessageDto {
    private long messageId;

    @Size(min = 1, max = 1000, message = "Content must be more than 1 character and less than 1000 characters.")
    @NotBlank(message = "Content is required.")
    private String content;
}
