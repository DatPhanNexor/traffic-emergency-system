package com.example.suco.exception;

public class AiRejectException extends RuntimeException {
    public AiRejectException(String message) {
        super(message);
    }
}
