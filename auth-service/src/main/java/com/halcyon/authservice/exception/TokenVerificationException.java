package com.halcyon.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenVerificationException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Authentication failure: Token missing, invalid, revoked or expired.";

    public TokenVerificationException() {
        super(DEFAULT_MESSAGE);
    }
}
