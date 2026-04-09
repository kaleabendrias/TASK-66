package com.demo.app.api.controller;

import com.demo.app.infrastructure.audit.Audited;
import com.demo.app.api.dto.ReservationRequest;
import com.demo.app.api.dto.StockReservationDto;
import com.demo.app.application.service.ReservationService;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.StockReservation;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @PostMapping
    @Audited(entityType = "RESERVATION", action = "RESERVE")
    public ResponseEntity<StockReservationDto> reserve(@Valid @RequestBody ReservationRequest request) {
        Long userId = getCurrentUserId();
        StockReservation reservation = reservationService.reserve(
                request.inventoryItemId(), userId, request.quantity(), request.idempotencyKey());
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/confirm")
    @Audited(entityType = "RESERVATION", action = "CONFIRM")
    public ResponseEntity<StockReservationDto> confirm(@PathVariable Long id) {
        enforceReservationOwnership(id);
        return ResponseEntity.ok(toDto(reservationService.confirm(id)));
    }

    @PostMapping("/{id}/cancel")
    @Audited(entityType = "RESERVATION", action = "CANCEL")
    public ResponseEntity<StockReservationDto> cancel(@PathVariable Long id) {
        enforceReservationOwnership(id);
        return ResponseEntity.ok(toDto(reservationService.cancel(id)));
    }

    @GetMapping("/my")
    public ResponseEntity<List<StockReservationDto>> myReservations() {
        Long userId = getCurrentUserId();
        List<StockReservationDto> reservations = reservationService.getUserHeldReservations(userId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    private void enforceReservationOwnership(Long reservationId) {
        StockReservation res = reservationService.getById(reservationId);
        Long currentUserId = getCurrentUserId();
        if (!res.getUserId().equals(currentUserId) && !isPrivileged()) {
            throw new OwnershipViolationException("Cannot modify another user's reservation");
        }
    }

    private boolean isPrivileged() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_WAREHOUSE_STAFF"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private StockReservationDto toDto(StockReservation r) {
        return new StockReservationDto(r.getId(), r.getInventoryItemId(), r.getUserId(), r.getQuantity(),
                r.getStatus() != null ? r.getStatus().name() : null, r.getIdempotencyKey(),
                r.getExpiresAt(), r.getCreatedAt(), r.getConfirmedAt(), r.getCancelledAt());
    }
}
