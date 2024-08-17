package com.halcyon.chatservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MessageNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Message with this id is not found.";

    public MessageNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
