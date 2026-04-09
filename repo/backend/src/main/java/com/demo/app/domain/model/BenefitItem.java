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
public class BenefitItem {

    private Long id;
    private Long packageId;
    private String benefitType;
    private String benefitValue;
    private String scope;
    private String exclusionGroup;
    private Long categoryId;
    private Long sellerId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
