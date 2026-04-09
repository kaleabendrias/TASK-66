package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank String content
) {}
