package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_score")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "factors", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String factors;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @Column(name = "seller_complaint_count", nullable = false)
    private int sellerComplaintCount;

    @Column(name = "open_incident_count", nullable = false)
    private int openIncidentCount;

    @Column(name = "appeal_rejection_count", nullable = false)
    private int appealRejectionCount;
}
