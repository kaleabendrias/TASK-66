package com.demo.app.domain.model;

import com.demo.app.domain.enums.FulfillmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fulfillment {
    private Long id;
    private Long orderId;
    private Long warehouseId;
    private FulfillmentStatus status;
    private Long operatorId;
    private String trackingInfo;
    private String idempotencyKey;
}
