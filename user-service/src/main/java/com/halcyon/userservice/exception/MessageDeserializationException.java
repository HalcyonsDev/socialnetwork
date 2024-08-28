package com.halcyon.userservice.exception;

public class MessageDeserializationException extends RuntimeException {
    public MessageDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
