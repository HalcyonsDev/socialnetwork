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
public class UpdatePostDto {
    private long postId;

    @Size(min = 1, max = 100, message = "Title must be more than 1 character and less than 100 characters.")
    private String title;

    @Size(min = 1, max = 5000, message = "\"About me\" must be more than 1 character and less than 5000 characters.")
    private String content;
}
