package com.demo.app.api.controller;

import com.demo.app.application.service.AccountDeletionService;
import com.demo.app.persistence.entity.AccountDeletionRequestEntity;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account-deletion")
@RequiredArgsConstructor
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestDeletion() {
        Long userId = getCurrentUserId();
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(userId);
        return ResponseEntity.ok(Map.of(
                "id", request.getId(),
                "status", request.getStatus(),
                "coolingOffEndsAt", request.getCoolingOffEndsAt().toString(),
                "message", "Account deletion requested. You have 30 days to cancel."
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelDeletion(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        AccountDeletionRequestEntity request = accountDeletionService.cancelDeletion(id, userId);
        return ResponseEntity.ok(Map.of(
                "id", request.getId(),
                "status", request.getStatus(),
                "message", "Account deletion cancelled."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Long userId = getCurrentUserId();
        AccountDeletionRequestEntity request = accountDeletionService.getByUser(userId);
        if (request == null) {
            return ResponseEntity.ok(Map.of("status", "NONE"));
        }
        return ResponseEntity.ok(Map.of(
                "id", request.getId(),
                "status", request.getStatus(),
                "coolingOffEndsAt", request.getCoolingOffEndsAt().toString()
        ));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
