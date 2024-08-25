package com.halcyon.authservice.exception;

public class MessageSerializationException extends RuntimeException {
    public MessageSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
