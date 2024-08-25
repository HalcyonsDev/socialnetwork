package com.halcyon.authservice.exception;

public class CookieDeserializationException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Failed to deserialize object from cookie.";

    public CookieDeserializationException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}
