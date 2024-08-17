package com.halcyon.chatservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MessageForbiddenException extends RuntimeException {
    public MessageForbiddenException(String message) {
        super(message);
    }
}
