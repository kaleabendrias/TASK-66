package com.demo.app.api.dto;

public record ReviewAppealRequest(
        String status,
        String reviewNotes
) {}
