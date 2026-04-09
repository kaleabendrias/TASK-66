package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_deletion_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "cooling_off_ends_at", nullable = false)
    private LocalDateTime coolingOffEndsAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
