package com.demo.app.api.controller;

import com.demo.app.infrastructure.audit.Audited;
import com.demo.app.api.dto.AppealDto;
import com.demo.app.api.dto.CreateAppealRequest;
import com.demo.app.api.dto.ReviewAppealRequest;
import com.demo.app.application.service.AppealService;
import com.demo.app.domain.exception.ConflictException;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.Appeal;
import com.demo.app.persistence.entity.AppealEvidenceEntity;
import com.demo.app.persistence.repository.AppealEvidenceRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final UserRepository userRepository;
    private final AppealEvidenceRepository appealEvidenceRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<List<AppealDto>> getPending() {
        List<AppealDto> appeals = appealService.getPending().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/my")
    public ResponseEntity<List<AppealDto>> getMy() {
        Long userId = getCurrentUserId();
        List<AppealDto> appeals = appealService.getByUser(userId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppealDto> getById(@PathVariable Long id) {
        Appeal appeal = appealService.getById(id);
        if (!isPrivileged() && !appeal.getUserId().equals(getCurrentUserId())) {
            throw new OwnershipViolationException("You do not have access to this appeal");
        }
        return ResponseEntity.ok(toDto(appeal));
    }

    @PostMapping
    @Audited(entityType = "APPEAL", action = "CREATE")
    public ResponseEntity<AppealDto> create(@Valid @RequestBody CreateAppealRequest request) {
        Long userId = getCurrentUserId();
        Appeal appeal = appealService.create(
                userId,
                request.relatedEntityType(),
                request.relatedEntityId(),
                request.reason()
        );
        return ResponseEntity.ok(toDto(appeal));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    @Audited(entityType = "APPEAL", action = "REVIEW")
    public ResponseEntity<AppealDto> review(@PathVariable Long id, @RequestBody ReviewAppealRequest request) {
        Long reviewerId = getCurrentUserId();
        Appeal appeal = appealService.review(id, reviewerId, request.status(), request.reviewNotes());
        return ResponseEntity.ok(toDto(appeal));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> uploadEvidence(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file) {
        // Ownership check
        Appeal appeal = appealService.getById(id);
        if (!isPrivileged() && !appeal.getUserId().equals(getCurrentUserId())) {
            throw new OwnershipViolationException("You do not have access to this appeal");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        long maxSize = 10 * 1024 * 1024; // 10 MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File exceeds maximum size of 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Only image and PDF files are allowed");
        }
        // Deep MIME validation via magic bytes
        try {
            byte[] header = new byte[8];
            java.io.InputStream is = file.getInputStream();
            int read = is.read(header);
            is.close();
            if (read >= 4) {
                boolean validMagic = false;
                // JPEG: FF D8 FF
                if (header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) validMagic = true;
                // PNG: 89 50 4E 47
                if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) validMagic = true;
                // GIF: 47 49 46
                if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) validMagic = true;
                // PDF: 25 50 44 46
                if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) validMagic = true;
                // WebP: 52 49 46 46 ... 57 45 42 50
                if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 && read >= 8
                    && header[4] == 0x57 && header[5] == 0x45 && header[6] == 0x42 && header[7] == 0x50) validMagic = true;
                if (!validMagic) {
                    throw new IllegalArgumentException("File content does not match an allowed type (image or PDF)");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to validate file content");
        }

        long existingCount = appealEvidenceRepository.countByAppealId(id);
        if (existingCount >= 5) {
            throw new ConflictException("Maximum 5 evidence files per appeal");
        }

        // Store file locally with sanitized filename
        String extension = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }
        // Sanitize: only allow alphanumeric extensions
        extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        String storedPath = "uploads/appeals/" + id + "/" + java.util.UUID.randomUUID() + extension;
        java.io.File dest = new java.io.File(storedPath);
        dest.getParentFile().mkdirs();
        try {
            file.transferTo(dest);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to store file");
        }

        AppealEvidenceEntity evidence = AppealEvidenceEntity.builder()
                .appealId(id)
                .originalName(file.getOriginalFilename())
                .storedPath(storedPath)
                .contentType(contentType)
                .fileSize(file.getSize())
                .uploadedAt(java.time.LocalDateTime.now())
                .build();
        appealEvidenceRepository.save(evidence);

        return ResponseEntity.ok(java.util.Map.of(
                "id", evidence.getId(),
                "originalName", evidence.getOriginalName(),
                "contentType", evidence.getContentType(),
                "fileSize", evidence.getFileSize()));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<?> getEvidence(@PathVariable Long id) {
        Appeal appeal = appealService.getById(id);
        if (!isPrivileged() && !appeal.getUserId().equals(getCurrentUserId())) {
            throw new OwnershipViolationException("You do not have access to this appeal");
        }
        List<Map<String, Object>> sanitized = appealEvidenceRepository.findByAppealId(id).stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("appealId", e.getAppealId());
                    m.put("originalName", e.getOriginalName());
                    m.put("contentType", e.getContentType());
                    m.put("fileSize", e.getFileSize());
                    m.put("uploadedAt", e.getUploadedAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(sanitized);
    }

    private boolean isPrivileged() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_MODERATOR"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

    private AppealDto toDto(Appeal a) {
        return new AppealDto(
                a.getId(),
                a.getUserId(),
                a.getRelatedEntityType(),
                a.getRelatedEntityId(),
                a.getReason(),
                a.getStatus(),
                a.getReviewerId(),
                a.getReviewNotes(),
                a.getCreatedAt(),
                a.getReviewedAt(),
                a.getResolvedAt()
        );
    }
}
