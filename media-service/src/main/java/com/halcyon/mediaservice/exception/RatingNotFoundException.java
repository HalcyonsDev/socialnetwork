package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RatingNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Rating with this id not found.";

    public RatingNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
