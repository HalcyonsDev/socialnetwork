package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PostForbiddentException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "You don't have the rights to change this post.";

    public PostForbiddentException() {
        super(DEFAULT_MESSAGE);
    }
}
