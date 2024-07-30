package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidVerificationCodeException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Invalid verification code.";

    public InvalidVerificationCodeException() {
        super(DEFAULT_MESSAGE);
    }
}
