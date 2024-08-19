package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEmailException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid email.";

    public InvalidEmailException() {
        super(DEFAULT_MESSAGE);
    }
}
