package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record BenefitItemDto(Long id, Long packageId, String benefitType, String benefitValue, String scope, String exclusionGroup, Long categoryId, Long sellerId, LocalDateTime validFrom, LocalDateTime validTo) {
}
