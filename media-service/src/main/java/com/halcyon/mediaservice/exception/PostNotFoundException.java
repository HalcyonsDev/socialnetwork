package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Post with this id not found.";

    public PostNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
