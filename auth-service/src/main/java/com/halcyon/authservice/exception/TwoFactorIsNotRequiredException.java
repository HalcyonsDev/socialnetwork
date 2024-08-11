package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TwoFactorIsNotRequiredException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Two-factor authentication is not required.";

    public TwoFactorIsNotRequiredException() {
        super(DEFAULT_MESSAGE);
    }
}
