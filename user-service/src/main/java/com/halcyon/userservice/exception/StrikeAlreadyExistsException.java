package com.halcyon.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StrikeAlreadyExistsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "You have already struck to this user.";

    public StrikeAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }
}
