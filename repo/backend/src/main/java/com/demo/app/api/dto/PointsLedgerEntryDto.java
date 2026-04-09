package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record PointsLedgerEntryDto(Long id, int amount, int spendAfter, String entryType, String reference,
                                   LocalDateTime createdAt) {
}
