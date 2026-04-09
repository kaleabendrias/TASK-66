package com.demo.app.persistence.entity;

import com.demo.app.domain.model.Appeal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "related_entity_type", nullable = false)
    private String relatedEntityType;

    @Column(name = "related_entity_id", nullable = false)
    private Long relatedEntityId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Appeal toModel() {
        return Appeal.builder()
                .id(id)
                .userId(userId)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .reason(reason)
                .status(status)
                .reviewerId(reviewerId)
                .reviewNotes(reviewNotes)
                .createdAt(createdAt)
                .reviewedAt(reviewedAt)
                .resolvedAt(resolvedAt)
                .build();
    }
}
