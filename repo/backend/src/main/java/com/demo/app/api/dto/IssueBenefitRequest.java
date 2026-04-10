package com.demo.app.api.dto;

public record IssueBenefitRequest(Long memberId, Long benefitItemId, String reference, String referenceType, Long referenceId) {
}
