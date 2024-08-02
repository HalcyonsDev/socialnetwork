package com.halcyon.mediaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnverifiedUserException extends RuntimeException {

    public UnverifiedUserException(String message) {
        super(message);
    }
}
