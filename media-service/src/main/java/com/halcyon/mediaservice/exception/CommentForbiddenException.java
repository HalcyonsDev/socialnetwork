package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CommentForbiddenException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "You don't have the rights to change this comment.";

    public CommentForbiddenException() {
        super(DEFAULT_MESSAGE);
    }
}
