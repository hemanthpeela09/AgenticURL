package com.example.shortener.api;

import org.springframework.http.HttpStatus;

/** Domain-level exception carrying an HTTP status for the error handler. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}