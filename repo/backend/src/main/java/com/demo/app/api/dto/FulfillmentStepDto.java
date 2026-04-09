package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record FulfillmentStepDto(Long id, Long fulfillmentId, String stepName, String status,
                                  Long operatorId, String notes, LocalDateTime createdAt,
                                  LocalDateTime completedAt) {
}
