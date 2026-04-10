package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.MemberProfileEntity;
import com.demo.app.persistence.entity.MemberTierEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.MemberProfileRepository;
import com.demo.app.persistence.repository.MemberTierRepository;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberProfileController - profile, phone, spend endpoints")
class MemberProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberTierRepository memberTierRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private String memberToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        UserEntity member = userRepository.save(TestFixtures.user("prof_member", Role.MEMBER));
        UserEntity admin = userRepository.save(TestFixtures.user("prof_admin", Role.ADMINISTRATOR));
        MemberTierEntity tier = memberTierRepository.save(TestFixtures.tier("Bronze", 1, 0, 999));

        memberProfileRepository.save(TestFixtures.profile(member.getId(), tier.getId(), 100));

        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());
        adminToken = jwtService.generateToken(admin.getUsername(), admin.getRole().name());
    }

    @Test
    @DisplayName("GET /members/me returns profile")
    void getMyProfile_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpend").value(100))
                .andExpect(jsonPath("$.tierName").value("Bronze"));
    }

    @Test
    @DisplayName("PUT /members/me/phone updates phone and returns masked")
    void updatePhone_returnsMasked() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("phone", "+15551234567"));
        mockMvc.perform(put("/api/members/me/phone").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneMasked").isNotEmpty());
    }

    @Test
    @DisplayName("POST /members/me/spend by admin adjusts spend")
    void adjustSpend_byAdmin_adjustsSpend() throws Exception {
        // Admin needs a profile too for getCurrentUserId -> addSpend(userId)
        MemberTierEntity tier = memberTierRepository.findAll().get(0);
        UserEntity admin = userRepository.findByUsername("prof_admin").orElseThrow();
        memberProfileRepository.save(TestFixtures.profile(admin.getId(), tier.getId(), 0));

        String body = objectMapper.writeValueAsString(Map.of("amount", 50, "reference", "bonus"));
        mockMvc.perform(post("/api/members/me/spend").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpend").value(50));
    }

    @Test
    @DisplayName("GET /members/me/spend/history returns list")
    void getSpendHistory_returnsList() throws Exception {
        mockMvc.perform(get("/api/members/me/spend/history").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
