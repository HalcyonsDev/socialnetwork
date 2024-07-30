package com.halcyon.jwtlibrary;

public class InvalidKeyException extends RuntimeException {
    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
