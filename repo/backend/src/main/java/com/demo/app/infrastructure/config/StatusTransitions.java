package com.demo.app.infrastructure.config;

import java.util.Map;
import java.util.Set;

public final class StatusTransitions {
    private StatusTransitions() {}

    public static final Map<String, Set<String>> INCIDENT = Map.of(
            "OPEN", Set.of("ACKNOWLEDGED"),
            "ACKNOWLEDGED", Set.of("IN_PROGRESS"),
            "IN_PROGRESS", Set.of("RESOLVED"),
            "RESOLVED", Set.of("CLOSED"),
            "CLOSED", Set.of()
    );

    public static final Map<String, Set<String>> APPEAL = Map.of(
            "SUBMITTED", Set.of("UNDER_REVIEW", "APPROVED", "REJECTED"),
            "UNDER_REVIEW", Set.of("APPROVED", "REJECTED"),
            "APPROVED", Set.of("CLOSED"),
            "REJECTED", Set.of("CLOSED"),
            "CLOSED", Set.of()
    );

    public static void validate(Map<String, Set<String>> matrix, String from, String to) {
        Set<String> allowed = matrix.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalStateException("Invalid transition from " + from + " to " + to);
        }
    }
}
