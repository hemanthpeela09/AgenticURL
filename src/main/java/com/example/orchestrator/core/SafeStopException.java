package com.example.orchestrator.core;

public class SafeStopException extends RuntimeException {
    public SafeStopException(String message) {
        super(message);
    }
}
