package com.demo.app.api.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {}
