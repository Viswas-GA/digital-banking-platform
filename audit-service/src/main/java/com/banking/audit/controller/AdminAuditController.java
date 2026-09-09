package com.banking.audit.controller;

import com.banking.audit.dto.AuditLogPageResponse;
import com.banking.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Audit Logs", description = "Admin-only immutable audit trail")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    @SecurityRequirements
    public Map<String, String> health() {
        return Map.of(
                "service", "audit-service",
                "status", "UP"
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search audit logs", description = "Admin only — paginated audit trail with optional filters")
    public AuditLogPageResponse search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditLogService.search(action, actorUserId, page, size);
    }
}
