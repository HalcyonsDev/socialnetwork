package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCredentialsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid credentials provided.";

    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }
}
