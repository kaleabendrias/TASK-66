package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.model.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false, insertable = false, updatable = false)
    private Long buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private UserEntity buyer;

    @Column(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "tender_type", nullable = false, length = 30)
    private String tenderType;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private java.math.BigDecimal refundAmount;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "reconciled", nullable = false)
    private boolean reconciled;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciliation_ref", length = 100)
    private String reconciliationRef;

    // The stock_reservation row that backs this order. Every PLACED order is
    // either created with a referenced HELD reservation or has one minted on
    // its behalf, so this is the contract handle the cancel/fail compensation
    // flow uses to release stock.
    @Column(name = "reservation_id")
    private Long reservationId;

    public Order toModel() {
        return Order.builder()
                .id(id)
                .buyerId(buyer != null ? buyer.getId() : buyerId)
                .productId(product != null ? product.getId() : productId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .status(status)
                .tenderType(tenderType)
                .refundAmount(refundAmount)
                .refundReason(refundReason)
                .reconciled(reconciled)
                .reconciledAt(reconciledAt)
                .reconciliationRef(reconciliationRef)
                .reservationId(reservationId)
                .build();
    }
}
