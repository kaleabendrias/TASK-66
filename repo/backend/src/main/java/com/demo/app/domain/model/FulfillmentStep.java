package com.demo.app.domain.model;

import com.demo.app.domain.enums.FulfillmentStepName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentStep {
    private Long id;
    private Long fulfillmentId;
    private FulfillmentStepName stepName;
    private String status;
    private Long operatorId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
