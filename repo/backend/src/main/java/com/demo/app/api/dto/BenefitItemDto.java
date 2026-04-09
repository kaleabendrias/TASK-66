package com.demo.app.api.dto;

public record BenefitItemDto(Long id, Long packageId, String benefitType, String benefitValue, String scope, String exclusionGroup) {
}
