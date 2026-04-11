package com.demo.app.application.service;

import com.demo.app.persistence.entity.RiskEventEntity;
import com.demo.app.persistence.entity.RiskScoreEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AppealRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.LoginAttemptRepository;
import com.demo.app.persistence.repository.RiskEventRepository;
import com.demo.app.persistence.repository.RiskScoreRepository;
import com.demo.app.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RiskAnalyticsService.class);

    private final RiskEventRepository riskEventRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final IncidentRepository incidentRepository;
    private final AppealRepository appealRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskEventEntity recordEvent(Long userId, String eventType, String severity, Map<String, Object> details) {
        String detailsJson;
        try {
            detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
        } catch (Exception e) {
            detailsJson = null;
        }

        RiskEventEntity event = RiskEventEntity.builder()
                .userId(userId)
                .eventType(eventType)
                .severity(severity)
                .details(detailsJson)
                .createdAt(LocalDateTime.now())
                .build();

        return riskEventRepository.save(event);
    }

    @Transactional
    public RiskScoreEntity computeScore(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<RiskEventEntity> recentEvents = riskEventRepository.findByUserIdAndCreatedAtAfter(userId, thirtyDaysAgo);

        double score = 0.0;
        Map<String, Object> factors = new HashMap<>();

        // Seller-scoped incident window: complaints filed *against* this user as
        // the seller in the last 30 days. This is the signal that matters for
        // marketplace risk — a seller getting repeatedly reported by buyers.
        // Reporter/assignee linkages (who filed, who triaged) are no longer
        // conflated with seller exposure.
        List<com.demo.app.persistence.entity.IncidentEntity> sellerRecentIncidents =
                incidentRepository.findBySellerIdSince(userId, thirtyDaysAgo);

        long sellerEscalatedIncidents = sellerRecentIncidents.stream()
                .filter(i -> i.getEscalationLevel() > 0)
                .count();
        factors.put("seller_escalated_incidents_30d", sellerEscalatedIncidents);
        score += sellerEscalatedIncidents * 15.0;

        long sellerHighSeverityIncidents = sellerRecentIncidents.stream()
                .filter(i -> "HIGH".equals(i.getSeverity()) || "EMERGENCY".equals(i.getSeverity()))
                .count();
        factors.put("seller_high_severity_incidents_30d", sellerHighSeverityIncidents);
        score += sellerHighSeverityIncidents * 12.0;

        // Repeat complaint signal: if a seller accumulates >2 incidents in 30d,
        // each additional incident stacks.
        long sellerIncidentCount = sellerRecentIncidents.size();
        long repeatSignal = sellerIncidentCount > 2 ? sellerIncidentCount - 2 : 0;
        factors.put("seller_repeat_incident_penalty", repeatSignal);
        score += repeatSignal * 8.0;

        factors.put("seller_total_incidents_30d", sellerIncidentCount);

        // Rejected appeals (staff exceptions denied)
        long rejectedAppeals = appealRepository.findByUserId(userId).stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .filter(a -> a.getResolvedAt() != null && a.getResolvedAt().isAfter(thirtyDaysAgo))
                .count();
        factors.put("rejected_appeals_30d", rejectedAppeals);
        score += rejectedAppeals * 10.0;

        // Failed logins (account compromise risk)
        UserEntity user = userRepository.findById(userId).orElse(null);
        long failedLogins = 0;
        if (user != null) {
            failedLogins = loginAttemptRepository.countRecentFailed(user.getUsername(), thirtyDaysAgo);
        }
        factors.put("failed_logins_30d", failedLogins);
        score += failedLogins * 3.0;

        // High severity risk events
        long highSeverityEvents = recentEvents.stream()
                .filter(e -> "HIGH".equals(e.getSeverity()) || "CRITICAL".equals(e.getSeverity()))
                .count();
        factors.put("high_severity_events_30d", highSeverityEvents);
        score += highSeverityEvents * 12.0;

        // Anomaly categories: specific operational risk signals
        long missedPickupCheckins = recentEvents.stream()
                .filter(e -> "MISSED_PICKUP_CHECKIN".equals(e.getEventType()))
                .count();
        factors.put("missed_pickup_checkins_30d", missedPickupCheckins);
        score += missedPickupCheckins * 10.0;

        long buddyPunchingEvents = recentEvents.stream()
                .filter(e -> "BUDDY_PUNCHING".equals(e.getEventType()))
                .count();
        factors.put("buddy_punching_30d", buddyPunchingEvents);
        score += buddyPunchingEvents * 20.0;

        long misidentificationEvents = recentEvents.stream()
                .filter(e -> "MISIDENTIFICATION".equals(e.getEventType()))
                .count();
        factors.put("misidentification_30d", misidentificationEvents);
        score += misidentificationEvents * 15.0;

        score = Math.min(score, 100.0);

        String factorsJson;
        try {
            factorsJson = objectMapper.writeValueAsString(factors);
        } catch (Exception e) {
            factorsJson = "{}";
        }

        RiskScoreEntity riskScore = riskScoreRepository.findByUserId(userId)
                .orElse(RiskScoreEntity.builder().userId(userId).build());

        long openSellerIncidents = sellerRecentIncidents.stream()
                .filter(i -> !"RESOLVED".equals(i.getStatus()) && !"CLOSED".equals(i.getStatus()))
                .count();

        riskScore.setScore(score);
        riskScore.setFactors(factorsJson);
        riskScore.setComputedAt(LocalDateTime.now());
        riskScore.setSellerComplaintCount((int) sellerIncidentCount);
        riskScore.setOpenIncidentCount((int) openSellerIncidents);
        riskScore.setAppealRejectionCount((int) rejectedAppeals);

        return riskScoreRepository.save(riskScore);
    }

    public RiskScoreEntity getScore(Long userId) {
        return riskScoreRepository.findByUserId(userId).orElse(null);
    }

    public List<RiskScoreEntity> getHighRiskUsers(double threshold) {
        return riskScoreRepository.findByScoreGreaterThanOrderByScoreDesc(threshold);
    }

    public List<RiskEventEntity> getEventsByUser(Long userId) {
        return riskEventRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
