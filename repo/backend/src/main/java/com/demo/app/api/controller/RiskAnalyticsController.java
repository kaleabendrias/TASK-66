package com.demo.app.api.controller;

import com.demo.app.application.service.RiskAnalyticsService;
import com.demo.app.persistence.entity.RiskEventEntity;
import com.demo.app.persistence.entity.RiskScoreEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequiredArgsConstructor
public class RiskAnalyticsController {

    private final RiskAnalyticsService riskAnalyticsService;

    @PostMapping("/compute/{userId}")
    public ResponseEntity<RiskScoreEntity> computeScore(@PathVariable Long userId) {
        return ResponseEntity.ok(riskAnalyticsService.computeScore(userId));
    }

    @GetMapping("/score/{userId}")
    public ResponseEntity<?> getScore(@PathVariable Long userId) {
        RiskScoreEntity score = riskAnalyticsService.getScore(userId);
        if (score == null) {
            return ResponseEntity.ok(Map.of("userId", userId, "score", 0.0, "message", "No risk score computed yet"));
        }
        return ResponseEntity.ok(score);
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<RiskScoreEntity>> getHighRiskUsers(
            @RequestParam(defaultValue = "50.0") double threshold) {
        return ResponseEntity.ok(riskAnalyticsService.getHighRiskUsers(threshold));
    }

    @GetMapping("/events/{userId}")
    public ResponseEntity<List<RiskEventEntity>> getEvents(@PathVariable Long userId) {
        return ResponseEntity.ok(riskAnalyticsService.getEventsByUser(userId));
    }
}
