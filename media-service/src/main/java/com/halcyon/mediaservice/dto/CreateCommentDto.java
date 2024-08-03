package com.halcyon.mediaservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDto {
    private long postId;

    @Size(min = 1, max = 500, message = "Content must be more than 1 character and less than 500 characters.")
    private String content;
}
