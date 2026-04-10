package com.demo.app.api.dto;

public record RedeemBenefitRequest(Long benefitItemId, String reference, Long categoryId, Long sellerId) {
}
