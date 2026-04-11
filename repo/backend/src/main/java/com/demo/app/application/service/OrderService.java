package com.demo.app.application.service;

import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.enums.ReservationStatus;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Order;
import com.demo.app.persistence.entity.InventoryItemEntity;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.StockReservationEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.InventoryItemRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.StockReservationRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle with server-authoritative pricing and a strict order ↔
 * reservation contract.
 *
 * <ul>
 *     <li>{@link #placeOrder} ignores client-supplied totals and recomputes
 *         them via {@link PricingService}. Every placed order is backed by a
 *         {@link StockReservationEntity} (either passed in by the caller or
 *         freshly minted).</li>
 *     <li>{@link #updateStatus} transitions the linked reservation in lockstep
 *         with the order: CONFIRMED confirms the hold, CANCELLED/FAILED
 *         release or roll it back depending on whether on-hand was already
 *         deducted.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ReservationService reservationService;
    private final PricingService pricingService;

    private static final Map<String, Set<String>> ORDER_TRANSITIONS = Map.of(
            "PLACED", Set.of("CONFIRMED", "CANCELLED", "FAILED"),
            "CONFIRMED", Set.of("SHIPPED", "CANCELLED", "FAILED"),
            "SHIPPED", Set.of("DELIVERED", "FAILED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of(),
            "FAILED", Set.of()
    );

    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .map(OrderEntity::toModel)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional(readOnly = true)
    public List<Order> getByBuyer(Long buyerId) {
        return orderRepository.findByBuyer_Id(buyerId).stream()
                .map(OrderEntity::toModel)
                .toList();
    }

    /**
     * Place a new order.
     *
     * <p>The {@code clientSuppliedTotal} parameter is captured only for
     * audit-log mismatch detection; the {@code totalPrice} written to the
     * database is always the {@link PricingService#computeOrderTotal} result.
     *
     * <p>If {@code reservationId} is non-null, the reservation must already
     * be HELD, owned by the buyer, and bound to an inventory item for the
     * same product. Otherwise, a HELD reservation is created on the buyer's
     * behalf using {@code inventoryItemIdHint} (or, if null, the first
     * inventory item we can find for the product with sufficient stock).
     *
     * <p>Runs in a single transaction with REPEATABLE_READ isolation so
     * concurrent placements cannot double-spend a hold.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order placeOrder(Long buyerId,
                            Long productId,
                            int quantity,
                            BigDecimal clientSuppliedTotal,
                            Long reservationId,
                            Long inventoryItemIdHint,
                            String idempotencyKey) {
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive, got: " + quantity);
        }

        // Server-authoritative pricing. The client-supplied total is captured
        // only so the controller can log a mismatch — never trusted.
        BigDecimal authoritativeTotal = pricingService.computeOrderTotal(buyerId, productId, quantity);

        // Resolve the backing reservation. Either re-use a caller-provided
        // HELD hold (after ownership/product checks), or create a new one.
        StockReservationEntity reservation;
        if (reservationId != null) {
            reservation = stockReservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
            validateExistingHold(reservation, buyerId, productId, quantity);
        } else {
            Long itemId = inventoryItemIdHint != null
                    ? inventoryItemIdHint
                    : pickInventoryItemForProduct(productId, quantity);
            String key = idempotencyKey != null
                    ? idempotencyKey
                    : "order-auto-" + buyerId + "-" + productId + "-" + System.nanoTime();
            reservationService.reserve(itemId, buyerId, quantity, key);
            // Re-fetch by idempotency key so we get the row id Hibernate just persisted.
            reservation = stockReservationRepository.findByIdempotencyKey(key)
                    .orElseThrow(() -> new IllegalStateException(
                            "Reservation persisted but not retrievable by key: " + key));
        }

        OrderEntity entity = OrderEntity.builder()
                .buyer(buyer)
                .product(product)
                .quantity(quantity)
                .totalPrice(authoritativeTotal)
                .status(OrderStatus.PLACED)
                .tenderType("INTERNAL_CREDIT")
                .reservationId(reservation.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return orderRepository.save(entity).toModel();
    }

    /** Backwards-compatible overload kept for existing service-layer call sites and unit tests. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order placeOrder(Order order) {
        return placeOrder(order.getBuyerId(), order.getProductId(), order.getQuantity(),
                order.getTotalPrice(), order.getReservationId(), null, null);
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        return updateStatus(id, newStatus, null);
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus, Long actorUserId) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        String current = entity.getStatus().name();
        String target = newStatus.name();

        if (current.equals(target)) {
            return entity.toModel();
        }

        Set<String> allowed = ORDER_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateException("Invalid order transition from " + current + " to " + target);
        }

        // Compensating reservation transitions in lockstep with the order.
        Long reservationId = entity.getReservationId();
        if (reservationId != null) {
            StockReservationEntity reservation = stockReservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
            String reservationStatus = reservation.getStatus();

            if (newStatus == OrderStatus.CONFIRMED) {
                // Confirm the hold → on_hand decreases.
                if (ReservationStatus.HELD.name().equals(reservationStatus)) {
                    reservationService.confirm(reservationId);
                }
            } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.FAILED) {
                // Compensate: release the hold (HELD) or roll back the
                // already-deducted on_hand (CONFIRMED/SHIPPED).
                if (ReservationStatus.HELD.name().equals(reservationStatus)) {
                    reservationService.cancel(reservationId);
                } else if (ReservationStatus.CONFIRMED.name().equals(reservationStatus)) {
                    reservationService.rollbackConfirmed(reservationId, actorUserId);
                }
            }
        }

        entity.setStatus(newStatus);
        entity.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(entity).toModel();
    }

    private void validateExistingHold(StockReservationEntity reservation,
                                      Long buyerId, Long productId, int quantity) {
        if (!ReservationStatus.HELD.name().equals(reservation.getStatus())) {
            throw new IllegalStateException(
                    "Referenced reservation is not HELD (status=" + reservation.getStatus() + ")");
        }
        if (!buyerId.equals(reservation.getUserId())) {
            throw new OwnershipViolationException(
                    "Reservation " + reservation.getId() + " is not held by user " + buyerId);
        }
        if (reservation.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Reservation " + reservation.getId() + " holds only "
                            + reservation.getQuantity() + " but order needs " + quantity);
        }
        InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory item", reservation.getInventoryItemId()));
        if (!productId.equals(item.getProductId())) {
            throw new IllegalArgumentException(
                    "Reservation " + reservation.getId()
                            + " is for product " + item.getProductId()
                            + " but order is for product " + productId);
        }
    }

    private Long pickInventoryItemForProduct(Long productId, int quantity) {
        return inventoryItemRepository.findByProductId(productId).stream()
                .filter(i -> (i.getQuantityOnHand() - i.getQuantityReserved()) >= quantity)
                .map(InventoryItemEntity::getId)
                .findFirst()
                .orElseThrow(() -> new com.demo.app.domain.exception.InsufficientStockException(0, quantity));
    }
}
