package com.banking.common.event;

import java.util.UUID;

public record AuditLogPayload(
        UUID actorUserId,
        String actorEmail,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String details
) {
}
