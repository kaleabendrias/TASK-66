package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_redemption_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitRedemptionLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "benefit_item_id", nullable = false)
    private Long benefitItemId;

    @Column(name = "reference")
    private String reference;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;
}
