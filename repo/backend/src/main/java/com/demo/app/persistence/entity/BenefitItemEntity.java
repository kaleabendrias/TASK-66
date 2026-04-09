package com.demo.app.persistence.entity;

import com.demo.app.domain.model.BenefitItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "benefit_type", nullable = false)
    private String benefitType;

    @Column(name = "benefit_value", nullable = false)
    private String benefitValue;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "exclusion_group", length = 50)
    private String exclusionGroup;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BenefitItem toModel() {
        return BenefitItem.builder()
                .id(id)
                .packageId(packageId)
                .benefitType(benefitType)
                .benefitValue(benefitValue)
                .scope(scope)
                .exclusionGroup(exclusionGroup)
                .build();
    }
}
