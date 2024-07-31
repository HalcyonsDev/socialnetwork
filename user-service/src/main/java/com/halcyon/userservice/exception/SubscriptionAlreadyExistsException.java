package com.halcyon.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SubscriptionAlreadyExistsException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "You have already subscribed to this user";

    public SubscriptionAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }
}
