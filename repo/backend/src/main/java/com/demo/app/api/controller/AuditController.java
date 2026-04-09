package com.demo.app.api.controller;

import com.demo.app.infrastructure.audit.AuditService;
import com.demo.app.persistence.entity.AuditLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogEntity>> getAuditLog(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(auditService.getByEntity(entityType, entityId));
    }
}
