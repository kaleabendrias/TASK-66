package com.demo.app.api.dto;

public record UserDto(
        Long id,
        String username,
        String email,
        String displayName,
        String role,
        boolean enabled
) {}
