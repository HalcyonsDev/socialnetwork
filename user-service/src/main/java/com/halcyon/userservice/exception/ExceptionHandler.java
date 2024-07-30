package com.halcyon.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandler extends ResponseEntityExceptionHandler {
    @org.springframework.web.bind.annotation.ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status;

        try {
            status = ex.getClass().getAnnotation(ResponseStatus.class).value();
        } catch (Exception ignored) {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(new ExceptionResponse(ex.getMessage()));
    }
}
