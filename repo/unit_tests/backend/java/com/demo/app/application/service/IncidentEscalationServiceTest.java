package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.IncidentEscalationLogEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.IncidentEscalationLogRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("IncidentEscalationService - SLA breach detection and escalation")
class IncidentEscalationServiceTest {

    @Autowired
    private IncidentEscalationService incidentEscalationService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEscalationLogRepository incidentEscalationLogRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity moderator;

    @BeforeEach
    void setUp() {
        moderator = userRepository.save(TestFixtures.user("moderator", Role.MODERATOR));
    }

    private IncidentEntity createOpenIncident(LocalDateTime slaAckDeadline, LocalDateTime slaResolveDeadline) {
        LocalDateTime now = LocalDateTime.now();
        return incidentRepository.save(IncidentEntity.builder()
                .reporterId(moderator.getId())
                .incidentType("FRAUD")
                .severity("HIGH")
                .title("Test Incident")
                .description("Test description")
                .status("OPEN")
                .escalationLevel(0)
                .slaAckDeadline(slaAckDeadline)
                .slaResolveDeadline(slaResolveDeadline)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Test
    @DisplayName("checkAndEscalate escalates unacknowledged incident past SLA to level 1")
    void testCheckAndEscalate_unacknowledgedPastSla_escalatesToLevel1() {
        IncidentEntity incident = createOpenIncident(
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusHours(24));

        incidentEscalationService.checkAndEscalate();

        IncidentEntity updated = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(1, updated.getEscalationLevel());
    }

    @Test
    @DisplayName("checkAndEscalate creates an escalation log entry")
    void testCheckAndEscalate_createsEscalationLog() {
        IncidentEntity incident = createOpenIncident(
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusHours(24));

        incidentEscalationService.checkAndEscalate();

        List<IncidentEscalationLogEntity> logs = incidentEscalationLogRepository.findByIncidentId(incident.getId());
        assertFalse(logs.isEmpty());
        assertEquals(0, logs.get(0).getFromLevel());
        assertEquals(1, logs.get(0).getToLevel());
        assertNotNull(logs.get(0).getReason());
    }

    @Test
    @DisplayName("checkAndEscalate escalates unresolved incident past resolve SLA to level 2")
    void testCheckAndEscalate_unresolvedPastResolveSla_escalatesToLevel2() {
        LocalDateTime now = LocalDateTime.now();
        IncidentEntity incident = incidentRepository.save(IncidentEntity.builder()
                .reporterId(moderator.getId())
                .incidentType("FRAUD")
                .severity("HIGH")
                .title("Unresolved Incident")
                .description("Test")
                .status("ACKNOWLEDGED")
                .escalationLevel(1)
                .slaAckDeadline(now.minusHours(1))
                .slaResolveDeadline(now.minusHours(1))
                .acknowledgedAt(now.minusMinutes(50))
                .createdAt(now.minusHours(2))
                .updatedAt(now)
                .build());

        incidentEscalationService.checkAndEscalate();

        IncidentEntity updated = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(2, updated.getEscalationLevel());
    }

    @Test
    @DisplayName("checkAndEscalate does not escalate when SLA is not breached")
    void testCheckAndEscalate_noBreaches_noEscalation() {
        IncidentEntity incident = createOpenIncident(
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now().plusHours(24));

        incidentEscalationService.checkAndEscalate();

        IncidentEntity updated = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(0, updated.getEscalationLevel());

        List<IncidentEscalationLogEntity> logs = incidentEscalationLogRepository.findByIncidentId(incident.getId());
        assertTrue(logs.isEmpty());
    }

    @Test
    @DisplayName("checkAndEscalate auto-assigns moderator user after escalation")
    void testCheckAndEscalate_autoAssignsModerator() {
        UserEntity reporter = userRepository.save(TestFixtures.user("escalreporter", Role.MEMBER));
        LocalDateTime now = LocalDateTime.now();
        IncidentEntity incident = incidentRepository.save(IncidentEntity.builder()
                .reporterId(reporter.getId())
                .incidentType("FRAUD")
                .severity("HIGH")
                .title("Unassigned Incident")
                .description("Needs auto-assignment")
                .status("OPEN")
                .escalationLevel(0)
                .slaAckDeadline(now.minusMinutes(20))
                .slaResolveDeadline(now.plusHours(24))
                .createdAt(now)
                .updatedAt(now)
                .build());

        incidentEscalationService.checkAndEscalate();

        IncidentEntity updated = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals(moderator.getId(), updated.getAssigneeId());
    }
}
