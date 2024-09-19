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
public class CreateMessageDto {
    @Size(min = 2, max = 1000, message = "Content must be more than 1 character and less than 1000 characters.")
    @NotBlank(message = "Content is required.")
    private String content;

    private Long recipientId;
}
