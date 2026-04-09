package com.demo.app.domain.model;

import com.demo.app.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
