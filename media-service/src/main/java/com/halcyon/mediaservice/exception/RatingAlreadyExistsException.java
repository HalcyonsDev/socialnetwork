package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RatingAlreadyExistsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "This user's rating for this post already exists.";

    public RatingAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }
}
