package com.halcyon.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRatingsResponse {
    private long postId;
    private int likesCount;
    private int dislikesCount;
}
