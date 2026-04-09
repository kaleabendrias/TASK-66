package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "spend_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "spend_after", nullable = false)
    private int spendAfter;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
