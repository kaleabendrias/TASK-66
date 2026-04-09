package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.FulfillmentStatus;
import com.demo.app.domain.model.Fulfillment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fulfillment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "tracking_info")
    private String trackingInfo;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Fulfillment toModel() {
        return Fulfillment.builder()
                .id(id)
                .orderId(orderId)
                .warehouseId(warehouseId)
                .status(FulfillmentStatus.valueOf(status))
                .operatorId(operatorId)
                .trackingInfo(trackingInfo)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
