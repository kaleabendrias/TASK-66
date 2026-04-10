package com.demo.app.infrastructure.scheduler;

import com.demo.app.application.service.AccountDeletionService;
import com.demo.app.application.service.IncidentEscalationService;
import com.demo.app.application.service.ListingService;
import com.demo.app.application.service.ReservationService;
import com.demo.app.infrastructure.audit.AuditService;
import com.demo.app.infrastructure.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final AuditService auditService;
    @Lazy
    private final ReservationService reservationService;
    @Lazy
    private final IncidentEscalationService incidentEscalationService;
    @Lazy
    private final AccountDeletionService accountDeletionService;
    private final RateLimitService rateLimitService;
    @Lazy
    private final ListingService listingService;

    @Scheduled(fixedRate = 60000)
    public void processExpiredReservations() {
        reservationService.expireOverdueReservations();
    }

    @Scheduled(fixedRate = 60000)
    public void checkIncidentSla() {
        incidentEscalationService.checkAndEscalate();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredAuditLogs() {
        auditService.purgeExpired();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void processExpiredDeletionRequests() {
        accountDeletionService.processExpired();
    }

    @Scheduled(fixedRate = 300000)
    public void evictRateLimitBuckets() {
        rateLimitService.evictExpired();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void refreshWeeklyViews() {
        listingService.refreshWeeklyViews();
    }
}
