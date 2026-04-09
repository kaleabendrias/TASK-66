package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.FulfillmentStepName;
import com.demo.app.domain.model.FulfillmentStep;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fulfillment_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fulfillment_id", nullable = false)
    private Long fulfillmentId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public FulfillmentStep toModel() {
        return FulfillmentStep.builder()
                .id(id)
                .fulfillmentId(fulfillmentId)
                .stepName(FulfillmentStepName.valueOf(stepName))
                .status(status)
                .operatorId(operatorId)
                .notes(notes)
                .createdAt(createdAt)
                .completedAt(completedAt)
                .build();
    }
}
