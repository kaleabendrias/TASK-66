package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.LoginAttemptRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController - login and registration endpoints")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private LoginAttemptRepository loginAttemptRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("authtest", Role.MEMBER));
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials returns 200 and token")
    void login_validCredentials_returnsToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "authtest", "password", "password123"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("authtest"));
    }

    @Test
    @DisplayName("POST /auth/login with wrong password returns 401")
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "authtest", "password", "wrongpass"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/register creates new user and returns token")
    void register_newUser_returnsToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "newuser", "email", "new@test.local",
                "password", "securepass", "displayName", "New User"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @DisplayName("POST /auth/register with duplicate username returns 400")
    void register_duplicateUsername_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "authtest", "email", "other@test.local",
                "password", "securepass", "displayName", "Dup User"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login lockout after 10 failures returns error")
    void login_lockoutAfterFailures_returnsError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "authtest", "password", "wrongpass"));
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("locked")));
    }
}
