package com.halcyon.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubscriptionNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Subscription with this owner and target is not found.";

    public SubscriptionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
