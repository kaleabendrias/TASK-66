package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AppealRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AppealController - appeal CRUD and authorization")
class AppealControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AppealRepository appealRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private String memberToken;
    private String moderatorToken;
    private String otherMemberToken;
    private UserEntity member;
    private UserEntity moderator;
    private UserEntity otherMember;
    private AppealEntity appeal;

    @BeforeEach
    void setUp() {
        member = userRepository.save(TestFixtures.user("appeal_member", Role.MEMBER));
        moderator = userRepository.save(TestFixtures.user("appeal_mod", Role.MODERATOR));
        otherMember = userRepository.save(TestFixtures.user("appeal_other", Role.MEMBER));

        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());
        moderatorToken = jwtService.generateToken(moderator.getUsername(), moderator.getRole().name());
        otherMemberToken = jwtService.generateToken(otherMember.getUsername(), otherMember.getRole().name());

        appeal = appealRepository.save(AppealEntity.builder()
                .userId(member.getId())
                .relatedEntityType("PRODUCT")
                .relatedEntityId(1L)
                .reason("Testing appeal")
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("POST /appeals with valid body returns 200 and SUBMITTED status")
    void create_validBody_returnsSubmitted() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "relatedEntityType", "ORDER", "relatedEntityId", 1, "reason", "My reason"));
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /appeals with empty relatedEntityType returns 400")
    void create_emptyEntityType_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "relatedEntityType", "", "relatedEntityId", 1, "reason", "test"));
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /appeals with blank reason returns 400")
    void create_blankReason_returnsBadRequest() throws Exception {
        String body = "{\"relatedEntityType\":\"PRODUCT\",\"relatedEntityId\":1,\"reason\":\"   \"}";
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /appeals requires MODERATOR role - member gets 403")
    void getPending_memberForbidden() throws Exception {
        mockMvc.perform(get("/api/appeals").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /appeals/my returns user's own appeals")
    void getMy_returnsOwnAppeals() throws Exception {
        mockMvc.perform(get("/api/appeals/my").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("Testing appeal"));
    }

    @Test
    @DisplayName("GET /{id} by owner returns 200")
    void getById_owner_returnsOk() throws Exception {
        mockMvc.perform(get("/api/appeals/" + appeal.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appeal.getId()));
    }

    @Test
    @DisplayName("GET /{id} by non-owner non-moderator returns 403")
    void getById_nonOwnerNonMod_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/appeals/" + appeal.getId())
                        .header("Authorization", "Bearer " + otherMemberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /{id}/review by moderator returns 200")
    void review_byModerator_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "APPROVED", "reviewNotes", "Looks good"));
        mockMvc.perform(post("/api/appeals/" + appeal.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /{id}/review by member returns 403")
    void review_byMember_returnsForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "APPROVED", "reviewNotes", "attempt"));
        mockMvc.perform(post("/api/appeals/" + appeal.getId() + "/review")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
