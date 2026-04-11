package com.demo.app.domain.enums;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    // Terminal failure (e.g. payment rejected, fulfillment irrecoverable).
    // Triggers the same compensation path as CANCELLED.
    FAILED
}
