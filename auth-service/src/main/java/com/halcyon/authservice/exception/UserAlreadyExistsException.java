package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserAlreadyExistsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "User with this email already exists.";

    public UserAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }
}
