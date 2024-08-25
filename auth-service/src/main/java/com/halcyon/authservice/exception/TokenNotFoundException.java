package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Authorization header is missing.";

    public TokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
