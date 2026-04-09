package com.demo.app.domain.exception;

public class OwnershipViolationException extends RuntimeException {
    public OwnershipViolationException(String message) {
        super(message);
    }
}
