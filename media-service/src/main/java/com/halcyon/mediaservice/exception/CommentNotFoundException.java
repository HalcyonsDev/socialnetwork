package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CommentNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Comment with this id not found.";

    public CommentNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
