package com.demo.app.api.controller;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AppealRepository;
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

import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private Long validIncidentId;

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

        IncidentEntity incident = incidentRepository.save(IncidentEntity.builder()
                .reporterId(member.getId())
                .incidentType("ORDER_ISSUE")
                .severity("NORMAL")
                .title("appeal test incident")
                .description("desc")
                .status("OPEN")
                .escalationLevel(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        validIncidentId = incident.getId();

        appeal = appealRepository.save(AppealEntity.builder()
                .userId(member.getId())
                .relatedEntityType("INCIDENT")
                .relatedEntityId(validIncidentId)
                .reason("Testing appeal")
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("POST /appeals with valid body returns 200 and SUBMITTED status")
    void create_validBody_returnsSubmitted() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "relatedEntityType", "INCIDENT", "relatedEntityId", validIncidentId, "reason", "My reason"));
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /appeals rejects relatedEntityId that does not exist")
    void create_missingRelatedEntity_returnsNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "relatedEntityType", "INCIDENT", "relatedEntityId", 9_999_999, "reason", "bogus"));
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /appeals rejects unknown relatedEntityType")
    void create_unknownRelatedEntityType_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "relatedEntityType", "WIDGET", "relatedEntityId", validIncidentId, "reason", "unknown type"));
        mockMvc.perform(post("/api/appeals").header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
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

    // ─────────────────────────────────────────────────────────────────
    // Multipart evidence upload — magic-byte and boundary validation
    // ─────────────────────────────────────────────────────────────────

    private static byte[] jpegBytes() {
        // Bare-minimum JPEG magic bytes — enough for the controller's header sniff.
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x01,
        };
    }

    private static byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R',
        };
    }

    private static byte[] webpBytes() {
        // RIFF/WEBP: 'RIFF' (4) + little-endian size (4) + 'WEBP' (4) + 'VP8 ' chunk.
        return new byte[] {
                'R', 'I', 'F', 'F',
                0x24, 0x00, 0x00, 0x00, // dummy size — these are the bytes the buggy
                // check was comparing against "WEBP" — they never match.
                'W', 'E', 'B', 'P',
                'V', 'P', '8', ' ',
        };
    }

    @Test
    @DisplayName("POST /{id}/evidence accepts a JPEG upload with proper multipart boundary")
    void uploadEvidence_acceptsJpeg() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegBytes());
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("photo.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("POST /{id}/evidence accepts a PNG upload")
    void uploadEvidence_acceptsPng() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "diagram.png", "image/png", pngBytes());
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    @DisplayName("POST /{id}/evidence accepts a valid RIFF/WEBP upload (regression: offset 8..11)")
    void uploadEvidence_acceptsValidWebP() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shot.webp", "image/webp", webpBytes());
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("image/webp"));
    }

    @Test
    @DisplayName("POST /{id}/evidence rejects content whose bytes don't match any allowed magic")
    void uploadEvidence_rejectsForgedContentType() throws Exception {
        // Claims image/png but the content is plain text.
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not a real image".getBytes());
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/evidence rejects an empty multipart part")
    void uploadEvidence_rejectsEmptyPart() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/evidence rejects a RIFF container whose chunk type is not WEBP")
    void uploadEvidence_rejectsRiffButNotWebp() throws Exception {
        byte[] riffWave = new byte[] {
                'R', 'I', 'F', 'F',
                0x24, 0x00, 0x00, 0x00,
                'W', 'A', 'V', 'E', // Not WEBP — must be rejected.
                'f', 'm', 't', ' ',
        };
        MockMultipartFile file = new MockMultipartFile(
                "file", "audio.webp", "image/webp", riffWave);
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/evidence rejects non-image content types (boundary fuzzing)")
    void uploadEvidence_rejectsDisallowedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[] { 'M', 'Z' });
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/evidence enforces the 5-file boundary per appeal")
    void uploadEvidence_enforcesFiveFileBoundary() throws Exception {
        // First five uploads must succeed.
        for (int i = 1; i <= 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evidence-" + i + ".jpg", "image/jpeg", jpegBytes());
            mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                            .file(file)
                            .header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isOk());
        }
        // The sixth upload must be rejected with a CONFLICT (409) — the
        // 5-files-per-appeal cap is a hard business invariant.
        MockMultipartFile sixth = new MockMultipartFile(
                "file", "evidence-6.jpg", "image/jpeg", jpegBytes());
        mockMvc.perform(multipart("/api/appeals/" + appeal.getId() + "/evidence")
                        .file(sixth)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /appeals/{id}/review rejects malformed payloads via Bean Validation")
    void review_rejectsBlankReviewNotes() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "APPROVED", "reviewNotes", ""));
        mockMvc.perform(post("/api/appeals/" + appeal.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /appeals/{id}/review rejects unsupported status verbs via Bean Validation")
    void review_rejectsUnknownStatus() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "MAYBE", "reviewNotes", "ok"));
        mockMvc.perform(post("/api/appeals/" + appeal.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
