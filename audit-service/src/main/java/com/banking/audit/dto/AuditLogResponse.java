package com.banking.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorEmail,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String details,
        Instant createdAt
) {
}
