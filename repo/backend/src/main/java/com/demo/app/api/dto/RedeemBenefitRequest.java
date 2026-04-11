package com.demo.app.api.dto;

/**
 * Redemption is scoped against a persisted order or incident. Category and
 * seller are intentionally NOT part of this DTO — they used to be client-
 * supplied "hints" that the service trusted, which let a member spoof an
 * unrelated category/seller to bypass a restricted benefit. The service now
 * resolves both fields from the persisted entity referenced by
 * {@code referenceType} + {@code referenceId}.
 */
public record RedeemBenefitRequest(Long benefitItemId, String reference, String referenceType, Long referenceId) {
}
