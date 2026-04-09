package com.demo.app.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int available, int requested) {
        super("Insufficient stock. Available: " + available + ", requested: " + requested);
    }
}
