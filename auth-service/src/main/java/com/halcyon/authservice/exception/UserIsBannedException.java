package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserIsBannedException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "You are banned.";

    public UserIsBannedException() {
        super(DEFAULT_MESSAGE);
    }
}
