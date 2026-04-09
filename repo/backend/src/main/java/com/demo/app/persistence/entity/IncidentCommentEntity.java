package com.demo.app.persistence.entity;

import com.demo.app.domain.model.IncidentComment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_comment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IncidentComment toModel() {
        return IncidentComment.builder()
                .id(id)
                .incidentId(incidentId)
                .authorId(authorId)
                .content(content)
                .createdAt(createdAt)
                .build();
    }
}
