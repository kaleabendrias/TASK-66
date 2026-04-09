package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_issuance_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitIssuanceLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "benefit_item_id", nullable = false)
    private Long benefitItemId;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @Column(name = "reference")
    private String reference;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
}
