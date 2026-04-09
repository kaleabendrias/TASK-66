package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record MemberProfileDto(Long id, Long userId, Long tierId, String tierName, int totalSpend, String phoneMasked,
                                LocalDateTime joinedAt) {
}
