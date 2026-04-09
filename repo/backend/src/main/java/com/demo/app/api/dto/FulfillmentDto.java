package com.demo.app.api.dto;

public record FulfillmentDto(Long id, Long orderId, Long warehouseId, String status,
                              Long operatorId, String trackingInfo, String idempotencyKey) {
}
