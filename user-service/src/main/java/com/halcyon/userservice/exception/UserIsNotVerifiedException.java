package com.halcyon.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserIsNotVerifiedException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "User is not verified. Please confirm your email.";

    public UserIsNotVerifiedException() {
        super(DEFAULT_MESSAGE);
    }
}
