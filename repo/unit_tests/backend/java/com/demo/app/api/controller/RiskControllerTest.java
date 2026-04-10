package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
@DisplayName("RiskAnalyticsController")
class RiskControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    private String adminToken, modToken, memberToken;
    private Long userId;

    @BeforeEach void setUp() {
        UserEntity admin = userRepository.save(TestFixtures.user("risk_admin", Role.ADMINISTRATOR));
        UserEntity mod = userRepository.save(TestFixtures.user("risk_mod", Role.MODERATOR));
        UserEntity member = userRepository.save(TestFixtures.user("risk_member", Role.MEMBER));
        adminToken = jwtService.generateToken(admin.getUsername(), admin.getRole().name());
        modToken = jwtService.generateToken(mod.getUsername(), mod.getRole().name());
        memberToken = jwtService.generateToken(member.getUsername(), member.getRole().name());
        userId = member.getId();
    }

    @Test void testComputeByAdmin() throws Exception {
        mockMvc.perform(post("/api/risk/compute/" + userId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").exists());
    }
    @Test void testComputeByModBlocked() throws Exception {
        mockMvc.perform(post("/api/risk/compute/" + userId).header("Authorization", "Bearer " + modToken))
                .andExpect(status().isForbidden());
    }
    @Test void testGetScore() throws Exception {
        mockMvc.perform(get("/api/risk/score/" + userId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
    @Test void testHighRisk() throws Exception {
        mockMvc.perform(get("/api/risk/high-risk").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
    @Test void testRecordEventByMod() throws Exception {
        mockMvc.perform(post("/api/risk/events").header("Authorization", "Bearer " + modToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userId + ",\"eventType\":\"MISSED_PICKUP_CHECKIN\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isOk());
    }
    @Test void testRecordEventByMemberBlocked() throws Exception {
        mockMvc.perform(post("/api/risk/events").header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userId + ",\"eventType\":\"TEST\",\"severity\":\"LOW\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void testGetEvents() throws Exception {
        mockMvc.perform(get("/api/risk/events/" + userId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
