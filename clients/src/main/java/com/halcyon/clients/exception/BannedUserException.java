package com.halcyon.clients.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BannedUserException extends RuntimeException {
    public BannedUserException(String message) {
        super(message);
    }
}
