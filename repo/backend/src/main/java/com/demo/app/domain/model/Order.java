package com.demo.app.domain.model;

import com.demo.app.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long buyerId;
    private Long productId;
    private int quantity;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String tenderType;
    private BigDecimal refundAmount;
    private String refundReason;
    private boolean reconciled;
    private LocalDateTime reconciledAt;
    private String reconciliationRef;
    private Long reservationId;
}
