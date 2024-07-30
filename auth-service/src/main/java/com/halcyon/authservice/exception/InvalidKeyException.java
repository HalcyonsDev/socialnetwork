package com.halcyon.authservice.exception;

public class InvalidKeyException extends RuntimeException {
    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
