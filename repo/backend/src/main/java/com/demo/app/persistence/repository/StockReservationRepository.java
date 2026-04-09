package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {
    Optional<StockReservationEntity> findByIdempotencyKey(String idempotencyKey);
    List<StockReservationEntity> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT r FROM StockReservationEntity r WHERE r.status = 'HELD' AND r.expiresAt < :now")
    List<StockReservationEntity> findExpired(@Param("now") LocalDateTime now);
}
