package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("IncidentController - incident lifecycle and access control")
class IncidentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private String memberToken;
    private String moderatorToken;
    private String otherMemberToken;
    private UserEntity member;
    private UserEntity moderator;
    private IncidentEntity incident;

    @BeforeEach
    void setUp() {
        member = userRepository.save(TestFixtures.user("inc_member", Role.MEMBER));
        moderator = userRepository.save(TestFixtures.user("inc_mod", Role.MODERATOR));
        UserEntity otherMember = userRepository.save(TestFixtures.user("inc_other", Role.MEMBER));

        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());
        moderatorToken = jwtService.generateToken(moderator.getUsername(), moderator.getRole().name());
        otherMemberToken = jwtService.generateToken(otherMember.getUsername(), otherMember.getRole().name());

        LocalDateTime now = LocalDateTime.now();
        incident = incidentRepository.save(IncidentEntity.builder()
                .reporterId(member.getId())
                .incidentType("ORDER_ISSUE")
                .severity("NORMAL")
                .title("Test incident")
                .description("Description")
                .status("OPEN")
                .escalationLevel(0)
                .slaAckDeadline(now.plusMinutes(15))
                .slaResolveDeadline(now.plusHours(24))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Test
    @DisplayName("POST /incidents with valid enum types returns 200")
    void create_validEnumTypes_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "incidentType", "ORDER_ISSUE", "severity", "NORMAL",
                "title", "New issue", "description", "Details"));
        mockMvc.perform(post("/api/incidents").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /incidents with address and crossStreet")
    void create_withAddressAndCrossStreet_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "incidentType", "EMERGENCY", "severity", "HIGH",
                "title", "Location issue", "description", "At corner",
                "address", "123 Main St", "crossStreet", "Oak Ave"));
        mockMvc.perform(post("/api/incidents").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.crossStreet").value("Oak Ave"));
    }

    @Test
    @DisplayName("GET /incidents/my returns own incidents")
    void getMy_returnsOwnIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents/my").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /{id}/acknowledge by moderator returns ACKNOWLEDGED")
    void acknowledge_byModerator_returnsAcknowledged() throws Exception {
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/acknowledge")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    @DisplayName("PATCH /{id}/status with closure code for RESOLVED")
    void updateStatus_resolvedWithClosureCode() throws Exception {
        // Move through valid transitions: OPEN -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED
        incidentRepository.findById(incident.getId()).ifPresent(e -> {
            e.setStatus("IN_PROGRESS");
            e.setAssigneeId(moderator.getId());
            incidentRepository.save(e);
        });
        String body = objectMapper.writeValueAsString(Map.of("status", "RESOLVED", "closureCode", "FIXED"));
        mockMvc.perform(patch("/api/incidents/" + incident.getId() + "/status")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("POST /{id}/comments adds comment")
    void addComment_addsComment() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "My comment"));
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/comments")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("My comment"));
    }

    @Test
    @DisplayName("GET /{id}/comments returns comments")
    void getComments_returnsList() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incident.getId() + "/comments")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /{id} by non-owner non-mod returns 403")
    void getById_nonOwnerNonMod_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incident.getId())
                        .header("Authorization", "Bearer " + otherMemberToken))
                .andExpect(status().isForbidden());
    }
}
