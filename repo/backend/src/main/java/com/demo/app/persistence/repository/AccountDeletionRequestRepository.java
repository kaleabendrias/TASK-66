package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.AccountDeletionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequestEntity, Long> {

    Optional<AccountDeletionRequestEntity> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT r FROM AccountDeletionRequestEntity r WHERE r.status = 'PENDING' AND r.coolingOffEndsAt < :now")
    List<AccountDeletionRequestEntity> findExpiredCoolingOff(@Param("now") LocalDateTime now);
}
