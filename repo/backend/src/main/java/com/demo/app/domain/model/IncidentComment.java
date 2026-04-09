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
public class IncidentComment {
    private Long id;
    private Long incidentId;
    private Long authorId;
    private String content;
    private LocalDateTime createdAt;
}
