package com.banking.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        String title,
        String message,
        UUID referenceId,
        boolean read,
        Instant createdAt
) {
}
