package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reviewer payload for an appeal decision. Status is restricted to the
 * exact verbs the {@code AppealService} state machine accepts; review notes
 * are required so an audit reader can always see why a decision was made.
 */
public record ReviewAppealRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "APPROVED|REJECTED|UNDER_REVIEW",
                message = "status must be one of APPROVED, REJECTED, UNDER_REVIEW")
        String status,
        @NotBlank(message = "reviewNotes is required so the decision is auditable")
        @Size(min = 3, max = 2000, message = "reviewNotes must be 3..2000 characters")
        String reviewNotes
) {}
