package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appeal {
    private Long id;
    private Long userId;
    private String relatedEntityType;
    private Long relatedEntityId;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;
}
