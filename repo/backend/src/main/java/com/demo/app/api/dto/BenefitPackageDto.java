package com.demo.app.api.dto;

public record BenefitPackageDto(Long id, Long tierId, String name, String description, boolean active) {
}
