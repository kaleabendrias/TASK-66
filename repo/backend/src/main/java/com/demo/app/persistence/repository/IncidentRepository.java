package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {

    List<IncidentEntity> findByReporterId(Long reporterId);

    List<IncidentEntity> findByAssigneeId(Long assigneeId);

    List<IncidentEntity> findBySellerId(Long sellerId);

    @Query("SELECT i FROM IncidentEntity i " +
           "WHERE i.sellerId = :sellerId AND i.createdAt IS NOT NULL AND i.createdAt > :since")
    List<IncidentEntity> findBySellerIdSince(@Param("sellerId") Long sellerId,
                                             @Param("since") LocalDateTime since);

    List<IncidentEntity> findByStatus(String status);

    @Query("SELECT i FROM IncidentEntity i WHERE i.status = 'OPEN' AND i.slaAckDeadline IS NOT NULL AND i.slaAckDeadline < :now")
    List<IncidentEntity> findUnacknowledgedPastSla(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM IncidentEntity i WHERE i.status IN ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS') AND i.slaResolveDeadline IS NOT NULL AND i.slaResolveDeadline < :now")
    List<IncidentEntity> findUnresolvedPastSla(@Param("now") LocalDateTime now);
}
