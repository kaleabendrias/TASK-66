package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
