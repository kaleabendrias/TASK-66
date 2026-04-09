package com.demo.app.persistence.entity;

import com.demo.app.domain.model.BenefitPackage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "benefit_package")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BenefitPackage toModel() {
        return BenefitPackage.builder()
                .id(id)
                .tierId(tierId)
                .name(name)
                .description(description)
                .active(active)
                .build();
    }
}
