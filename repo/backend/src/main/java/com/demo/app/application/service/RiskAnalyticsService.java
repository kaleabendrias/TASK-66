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

        // Failed logins: look up by username, not userId
        UserEntity user = userRepository.findById(userId).orElse(null);
        long failedLogins = 0;
        if (user != null) {
            failedLogins = loginAttemptRepository.countRecentFailed(user.getUsername(), LocalDateTime.now().minusDays(30));
        }
        factors.put("failed_logins_30d", failedLogins);
        score += failedLogins * 5.0;

        // Open incidents reported against or by this user
        long openIncidents = incidentRepository.findByReporterId(userId).stream()
                .filter(i -> "OPEN".equals(i.getStatus()) || "ACKNOWLEDGED".equals(i.getStatus()))
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        factors.put("open_incidents", openIncidents);
        score += openIncidents * 10.0;

        // Rejected appeals
        long rejectedAppeals = appealRepository.findByUserId(userId).stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .filter(a -> a.getResolvedAt() != null && a.getResolvedAt().isAfter(thirtyDaysAgo))
                .count();
        factors.put("rejected_appeals", rejectedAppeals);
        score += rejectedAppeals * 8.0;

        // High severity risk events
        long highSeverityEvents = recentEvents.stream()
                .filter(e -> "HIGH".equals(e.getSeverity()) || "CRITICAL".equals(e.getSeverity()))
                .count();
        factors.put("high_severity_events_30d", highSeverityEvents);
        score += highSeverityEvents * 15.0;

        long totalEvents = recentEvents.size();
        factors.put("total_events_30d", totalEvents);
        score += totalEvents * 2.0;

        score = Math.min(score, 100.0);

        String factorsJson;
        try {
            factorsJson = objectMapper.writeValueAsString(factors);
        } catch (Exception e) {
            factorsJson = "{}";
        }

        RiskScoreEntity riskScore = riskScoreRepository.findByUserId(userId)
                .orElse(RiskScoreEntity.builder().userId(userId).build());

        riskScore.setScore(score);
        riskScore.setFactors(factorsJson);
        riskScore.setComputedAt(LocalDateTime.now());
        riskScore.setSellerComplaintCount((int) openIncidents);
        riskScore.setOpenIncidentCount((int) openIncidents);
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
